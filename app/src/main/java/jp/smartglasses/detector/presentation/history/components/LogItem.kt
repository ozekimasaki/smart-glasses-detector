package jp.smartglasses.detector.presentation.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import jp.smartglasses.detector.R
import jp.smartglasses.detector.domain.model.DetectionLog
import jp.smartglasses.detector.ui.theme.DistanceClose
import jp.smartglasses.detector.ui.theme.DistanceFar
import jp.smartglasses.detector.ui.theme.DistanceMedium
import jp.smartglasses.detector.ui.theme.DistanceVeryClose
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogItem(
    log: DetectionLog,
    modifier: Modifier = Modifier
) {
    val (badgeColor, distanceLabel) = resolveDistance(log.distance)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // アイコン（ブランドカラーのアクセント、背景は薄いグレー）
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Sensors,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.manufacturerName.ifEmpty { log.deviceName.ifEmpty { "不明な機器" } },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatTime(log.detectedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 距離バッジ（コントラスト比を確保した固定色）
            Text(
                text = distanceLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeColor)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun resolveDistance(distance: String): Pair<Color, String> {
    return when {
        distance.contains("とても近") ->
            DistanceVeryClose to stringResource(R.string.distance_very_close)
        distance.contains("近") ->
            DistanceClose to stringResource(R.string.distance_close)
        distance.contains("少し") ->
            DistanceMedium to stringResource(R.string.distance_medium)
        else ->
            DistanceFar to stringResource(R.string.distance_far)
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
