package jp.smartglasses.detector.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import jp.smartglasses.detector.R
import jp.smartglasses.detector.util.ScanSensitivity

@Composable
fun SensitivitySelector(
    selected: ScanSensitivity,
    onSelect: (ScanSensitivity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
    ) {
        SensitivityOption(
            label = stringResource(R.string.settings_sensitivity_balanced),
            description = stringResource(R.string.settings_sensitivity_balanced_desc),
            selected = selected == ScanSensitivity.BALANCED,
            onClick = { onSelect(ScanSensitivity.BALANCED) }
        )
        SensitivityOption(
            label = stringResource(R.string.settings_sensitivity_low),
            description = stringResource(R.string.settings_sensitivity_low_desc),
            selected = selected == ScanSensitivity.LOW_POWER,
            onClick = { onSelect(ScanSensitivity.LOW_POWER) }
        )
        SensitivityOption(
            label = stringResource(R.string.settings_sensitivity_high),
            description = stringResource(R.string.settings_sensitivity_high_desc),
            selected = selected == ScanSensitivity.HIGH_ACCURACY,
            onClick = { onSelect(ScanSensitivity.HIGH_ACCURACY) }
        )
    }
}

@Composable
private fun SensitivityOption(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (selected) {
                    Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(1.dp, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier
                }
            )
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
