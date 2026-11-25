//! monitor-core: native metrics history engine (ring buffer, sparkline
//! downsampling, window statistics) for the HP Monitor app.
//!
//! The Rust side owns only pure computation. All state lives in one global,
//! mutex-guarded store — the Android app is single-process and uses this
//! crate exclusively. Floating-point results cross JNI as little-endian
//! byte arrays (stable, version-proof ABI).

use jni::objects::{JByteArray, JClass, JObject};
use jni::sys::{jboolean, jfloat, jint, jlong};
use jni::JNIEnv;
use std::collections::VecDeque;
use std::sync::{Mutex, OnceLock};

const CAPACITY_DEFAULT: usize = 360;

#[derive(Clone, Copy)]
struct Sample {
    ts: i64,
    cpu: f32,
    mem: f32,
    swap: f32,
    rx: i64,
    tx: i64,
    batt: f32,
    temp: f32,
}

impl Sample {
    fn value(&self, metric: i32) -> f64 {
        match metric {
            0 => self.cpu as f64,
            1 => self.mem as f64,
            2 => self.swap as f64,
            3 => self.rx as f64,
            4 => self.tx as f64,
            5 => self.batt as f64,
            6 => self.temp as f64,
            _ => 0.0,
        }
    }
}

struct Store {
    capacity: usize,
    data: VecDeque<Sample>,
}

fn store() -> &'static Mutex<Store> {
    static STORE: OnceLock<Mutex<Store>> = OnceLock::new();
    STORE.get_or_init(|| {
        Mutex::new(Store {
            capacity: CAPACITY_DEFAULT,
            data: VecDeque::with_capacity(CAPACITY_DEFAULT),
        })
    })
}

/// Bucketed [avg, min, max] per point over the stored history.
fn metric_sparkline(data: &[Sample], metric: i32, points: i32) -> Vec<f32> {
    let n = data.len();
    if n == 0 || points <= 0 {
        return Vec::new();
    }
    let points = (points as usize).min(n);
    let bucket = (n + points - 1) / points;
    let mut out: Vec<f32> = Vec::with_capacity(((n + bucket - 1) / bucket) * 3);
    let mut start = 0usize;
    while start < n {
        let end = (start + bucket).min(n);
        let mut sum = 0.0f64;
        let mut mn = f64::MAX;
        let mut mx = f64::MIN;
        for s in &data[start..end] {
            let v = s.value(metric);
            sum += v;
            if v < mn {
                mn = v;
            }
            if v > mx {
                mx = v;
            }
        }
        let cnt = (end - start) as f64;
        out.push((sum / cnt) as f32);
        out.push(mn as f32);
        out.push(mx as f32);
        start = end;
    }
    out
}

/// [count, last, avg, min, max]
fn metric_stats(data: &[Sample], metric: i32) -> Vec<f32> {
    let n = data.len();
    if n == 0 {
        return vec![0.0, 0.0, 0.0, 0.0, 0.0];
    }
    let mut sum = 0.0f64;
    let mut mn = f64::MAX;
    let mut mx = f64::MIN;
    for s in data {
        let v = s.value(metric);
        sum += v;
        if v < mn {
            mn = v;
        }
        if v > mx {
            mx = v;
        }
    }
    let last = data[n - 1].value(metric);
    vec![n as f32, last as f32, (sum / n as f64) as f32, mn as f32, mx as f32]
}

fn f32s_to_le_bytes(v: &[f32]) -> Vec<u8> {
    let mut out = Vec::with_capacity(v.len() * 4);
    for x in v {
        out.extend_from_slice(&x.to_le_bytes());
    }
    out
}

fn to_jbyte_array(env: &JNIEnv, bytes: &[u8]) -> JObject {
    match env.byte_array_from_slice(bytes) {
        Ok(arr) => {
            let raw: JByteArray = arr;
            raw.into_raw()
        }
        Err(_) => JObject::null(),
    }
}

// ---------------------------------------------------------------------------
// JNI entry points — class com.sahalos.hpmonitor.core.NativeHistory
// ---------------------------------------------------------------------------

#[no_mangle]
pub extern "system" fn Java_com_sahalos_hpmonitor_core_NativeHistory_nativeInit(
    _env: JNIEnv,
    _class: JClass,
    capacity: jint,
) -> jboolean {
    let mut g = match store().lock() {
        Ok(g) => g,
        Err(_) => return 0,
    };
    g.capacity = if capacity > 0 {
        (capacity as usize).min(10_000)
    } else {
        CAPACITY_DEFAULT
    };
    g.data.clear();
    g.data.reserve(g.capacity);
    1
}

#[no_mangle]
pub extern "system" fn Java_com_sahalos_hpmonitor_core_NativeHistory_nativePush(
    _env: JNIEnv,
    _class: JClass,
    ts: jlong,
    cpu: jfloat,
    mem: jfloat,
    swap: jfloat,
    rx: jlong,
    tx: jlong,
    batt: jfloat,
    temp: jfloat,
) {
    let g = match store().lock() {
        Ok(g) => g,
        Err(_) => return,
    };
    g.data.push_back(Sample {
        ts,
        cpu,
        mem,
        swap,
        rx,
        tx,
        batt,
        temp,
    });
    while g.data.len() > g.capacity {
        g.data.pop_front();
    }
}

#[no_mangle]
pub extern "system" fn Java_com_sahalos_hpmonitor_core_NativeHistory_nativeSparkline(
    env: JNIEnv,
    _class: JClass,
    metric: jint,
    points: jint,
) -> JObject {
    let buf = {
        let g = match store().lock() {
            Ok(g) => g,
            Err(_) => return JObject::null(),
        };
        metric_sparkline(&g.data, metric, points)
    };
    to_jbyte_array(&env, &f32s_to_le_bytes(&buf))
}

#[no_mangle]
pub extern "system" fn Java_com_sahalos_hpmonitor_core_NativeHistory_nativeStats(
    env: JNIEnv,
    _class: JClass,
    metric: jint,
) -> JObject {
    let buf = {
        let g = match store().lock() {
            Ok(g) => g,
            Err(_) => return JObject::null(),
        };
        metric_stats(&g.data, metric)
    };
    to_jbyte_array(&env, &f32s_to_le_bytes(&buf))
}

#[no_mangle]
pub extern "system" fn Java_com_sahalos_hpmonitor_core_NativeHistory_nativeClear(
    _env: JNIEnv,
    _class: JClass,
) {
    if let Ok(mut g) = store().lock() {
        g.data.clear();
    }
}

#[no_mangle]
pub extern "system" fn Java_com_sahalos_hpmonitor_core_NativeHistory_nativeCount(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    match store().lock() {
        Ok(g) => g.data.len() as jint,
        Err(_) => 0,
    }
}
