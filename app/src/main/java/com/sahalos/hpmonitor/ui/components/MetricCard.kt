package com.sahalos.hpmonitor.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sahalos.hpmonitor.ui.theme.MonitorSurface
import com.sahalos.hpmonitor.ui.theme.TextSecondary

@Composable
fun MetricCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier.fillMaxWidth(),
    compact: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        shape = RoundedCornerShape(if (compact) 8.dp else 12.dp),
        colors = CardDefaults.cardColors(containerColor = MonitorSurface),
        modifier = modifier,
    ) {
        Column(Modifier.padding(if (compact) 8.dp else 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(if (compact) 15.dp else 18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = title,
                    style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                )
            }
            Spacer(Modifier.height(if (compact) 6.dp else 12.dp))
            content()
        }
    }
}
