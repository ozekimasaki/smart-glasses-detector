package jp.smartglasses.detector.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import jp.smartglasses.detector.R
import jp.smartglasses.detector.presentation.components.BottomNavigationBar
import jp.smartglasses.detector.presentation.navigation.Screen
import jp.smartglasses.detector.presentation.settings.components.SensitivitySelector
import jp.smartglasses.detector.util.BackgroundScanSupport

@Composable
fun SettingsScreen(
    onNavigate: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val backgroundEnabled by viewModel.backgroundEnabled.collectAsStateWithLifecycle()
    val notificationEnabled by viewModel.notificationEnabled.collectAsStateWithLifecycle()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
    val sensitivity by viewModel.sensitivity.collectAsStateWithLifecycle()
    val backgroundSupported = BackgroundScanSupport.isSupported()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentRoute = Screen.Settings.route,
                onNavigate = onNavigate
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingsGroup(label = stringResource(R.string.settings_group_behavior)) {
                SettingSwitch(
                    title = stringResource(R.string.settings_background),
                    subtitle = stringResource(
                        if (backgroundSupported) {
                            R.string.settings_background_desc_supported
                        } else {
                            R.string.settings_background_desc_unsupported
                        }
                    ),
                    checked = backgroundSupported && backgroundEnabled,
                    enabled = backgroundSupported,
                    onCheckedChange = viewModel::setBackgroundEnabled
                )
                SettingDivider()
                SettingSwitch(
                    title = stringResource(R.string.settings_notification),
                    subtitle = stringResource(R.string.settings_notification_desc),
                    checked = notificationEnabled,
                    onCheckedChange = viewModel::setNotificationEnabled
                )
                SettingDivider()
                SettingSwitch(
                    title = stringResource(R.string.settings_vibration),
                    subtitle = stringResource(R.string.settings_vibration_desc),
                    checked = vibrationEnabled,
                    onCheckedChange = viewModel::setVibrationEnabled
                )
                SettingDivider()
                SettingSwitch(
                    title = stringResource(R.string.settings_sound),
                    subtitle = stringResource(R.string.settings_sound_desc),
                    checked = soundEnabled,
                    onCheckedChange = viewModel::setSoundEnabled
                )
            }

            SettingsGroup(label = stringResource(R.string.settings_group_sensitivity)) {
                SensitivitySelector(
                    selected = sensitivity,
                    onSelect = viewModel::setSensitivity,
                    modifier = Modifier.padding(8.dp)
                )
            }

            SettingsGroup(label = stringResource(R.string.settings_group_other)) {
                SettingNavRow(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.settings_about),
                    onClick = { onNavigate(Screen.About.route) }
                )
                SettingDivider()
                SettingNavRow(
                    icon = Icons.Outlined.Policy,
                    title = stringResource(R.string.settings_privacy),
                    onClick = { onNavigate(Screen.Privacy.route) }
                )
            }
        }
    }
}

// ─── セクション枠（白カード + ラベル）
@Composable
private fun SettingsGroup(
    label: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}

@Composable
private fun SettingDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                enabled = enabled,
                role = Role.Switch
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled
        )
    }
}

@Composable
private fun SettingNavRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}
