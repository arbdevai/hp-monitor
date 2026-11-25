package com.sahalos.hpmonitor.core

/** Metric indices shared between the Rust engine and the Kotlin layer. */
object Metric {
    const val CPU = 0
    const val MEM = 1
    const val SWAP = 2
    const val RX = 3
    const val TX = 4
    const val BATTERY = 5
    const val TEMP = 6
    const val COUNT = 7
}
