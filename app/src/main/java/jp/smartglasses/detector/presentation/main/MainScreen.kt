package jp.smartglasses.detector.presentation.main

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import jp.smartglasses.detector.R
import jp.smartglasses.detector.presentation.components.BottomNavigationBar
import jp.smartglasses.detector.presentation.navigation.Screen
import jp.smartglasses.detector.ui.theme.BrandOrange

@Composable
fun MainScreen(
    onNavigate: (String) -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val todayCount by viewModel.todayCount.collectAsStateWithLifecycle()
    val backgroundScanningEnabled by viewModel.backgroundScanningEnabled.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is MainEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                MainEvent.OpenAppSettings -> {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentRoute = Screen.Main.route,
                onNavigate = onNavigate
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // ─── アプリ説明ヘッダー ───
            AppDescription()

            Spacer(modifier = Modifier.height(28.dp))

            // ─── 今日の実績 ───
            TodayStats(todayCount = todayCount)

            Spacer(modifier = Modifier.height(20.dp))

            // ─── スキャン状態バッジ（スキャン中のみ） ───
            AnimatedContent(
                targetState = isScanning,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "status_badge"
            ) { scanning ->
                if (scanning) {
                    ScanningStatusBadge(canContinueInBackground = backgroundScanningEnabled)
                } else {
                    Spacer(modifier = Modifier.height(0.dp))
                }
            }

            if (isScanning) Spacer(modifier = Modifier.height(20.dp))

            // ─── アクションボタン ───
            ScanActionButton(
                isScanning = isScanning,
                onClick = { viewModel.toggleScanning() }
            )

            // ─── 使い方ヒント（非スキャン時のみ） ───
            if (!isScanning) {
                Spacer(modifier = Modifier.height(28.dp))
                HowItWorks()
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// アプリの目的を最初に明示するヘッダー
// ─────────────────────────────────────────────────────────────────
@Composable
private fun AppDescription() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BrandOrange)
        ) {
            Icon(
                imageVector = Icons.Outlined.Sensors,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = stringResource(R.string.main_app_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.main_app_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 説明本文
    Text(
        text = stringResource(R.string.main_headline),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = 38.sp
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = stringResource(R.string.main_description),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 22.sp
    )
}

// ─────────────────────────────────────────────────────────────────
// 今日の実績カード
// ─────────────────────────────────────────────────────────────────
@Composable
private fun TodayStats(todayCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$todayCount",
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                color = BrandOrange
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.main_count_unit),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.main_count_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// スキャン中ステータスバッジ（小さな状態表示）
// ─────────────────────────────────────────────────────────────────
@Composable
private fun ScanningStatusBadge(canContinueInBackground: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        // 点滅ドット
        val transition = rememberInfiniteTransition(label = "dot")
        val dotAlpha by transition.animateFloat(
            initialValue = 1f, targetValue = 0.2f,
            animationSpec = infiniteRepeatable(
                tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse
            ),
            label = "dot_alpha"
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(BrandOrange.copy(alpha = dotAlpha))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(
                if (canContinueInBackground) {
                    R.string.main_scanning_badge_background
                } else {
                    R.string.main_scanning_badge_foreground
                }
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// メインアクションボタン（横長・ソリッド）
// ─────────────────────────────────────────────────────────────────
@Composable
private fun ScanActionButton(
    isScanning: Boolean,
    onClick: () -> Unit
) {
    AnimatedContent(
        targetState = isScanning,
        transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(350)) },
        label = "action_btn"
    ) { scanning ->
        if (scanning) {
            val infiniteTransition = rememberInfiniteTransition(label = "ripple")
            val ripple1Scale by infiniteTransition.animateFloat(
                initialValue = 1f, targetValue = 1.5f,
                animationSpec = infiniteRepeatable(
                    tween(1200, easing = FastOutSlowInEasing), RepeatMode.Restart
                ), label = "r1s"
            )
            val ripple1Alpha by infiniteTransition.animateFloat(
                initialValue = 0.35f, targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    tween(1200, easing = FastOutSlowInEasing), RepeatMode.Restart
                ), label = "r1a"
            )
            val ripple2Scale by infiniteTransition.animateFloat(
                initialValue = 1f, targetValue = 1.5f,
                animationSpec = infiniteRepeatable(
                    tween(1200, delayMillis = 450, easing = FastOutSlowInEasing), RepeatMode.Restart
                ), label = "r2s"
            )
            val ripple2Alpha by infiniteTransition.animateFloat(
                initialValue = 0.35f, targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    tween(1200, delayMillis = 450, easing = FastOutSlowInEasing), RepeatMode.Restart
                ), label = "r2a"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onClick() }
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .scale(ripple1Scale)
                            .clip(CircleShape)
                            .background(BrandOrange.copy(alpha = ripple1Alpha))
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .scale(ripple2Scale)
                            .clip(CircleShape)
                            .background(BrandOrange.copy(alpha = ripple2Alpha))
                    )
                    Icon(
                        imageVector = Icons.Filled.StopCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(R.string.main_stop_button),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.main_stop_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // ─── 開始ボタン ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BrandOrange)
                    .clickable { onClick() }
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bluetooth,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(R.string.main_start_button),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.main_start_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// 仕組みの説明（3行）
// ─────────────────────────────────────────────────────────────────
@Composable
private fun HowItWorks() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.main_about_section),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HintRow(icon = Icons.Outlined.Bluetooth, text = stringResource(R.string.main_hint_scan))
        HintRow(icon = Icons.Outlined.Sensors, text = stringResource(R.string.main_hint_notify))
        HintRow(icon = Icons.Outlined.History, text = stringResource(R.string.main_hint_history))
    }
}

@Composable
private fun HintRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )
    }
}
