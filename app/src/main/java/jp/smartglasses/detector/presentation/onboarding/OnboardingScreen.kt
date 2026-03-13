package jp.smartglasses.detector.presentation.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import jp.smartglasses.detector.R
import jp.smartglasses.detector.util.BackgroundScanSupport

private sealed class OnboardingStep {
    object Welcome : OnboardingStep()
    object HowTo : OnboardingStep()
    object Permission : OnboardingStep()
}

private val stepOrder = listOf(
    OnboardingStep.Welcome,
    OnboardingStep.HowTo,
    OnboardingStep.Permission
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    var currentStep by remember { mutableStateOf<OnboardingStep>(OnboardingStep.Welcome) }
    val currentIndex = stepOrder.indexOf(currentStep)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (currentStep) {
            is OnboardingStep.Welcome -> WelcomeStep(
                onNext = { currentStep = OnboardingStep.HowTo },
                modifier = Modifier.weight(1f)
            )
            is OnboardingStep.HowTo -> HowToStep(
                onNext = { currentStep = OnboardingStep.Permission },
                modifier = Modifier.weight(1f)
            )
            is OnboardingStep.Permission -> PermissionStep(
                onComplete = onComplete,
                modifier = Modifier.weight(1f)
            )
        }

        StepIndicator(
            currentIndex = currentIndex,
            total = stepOrder.size,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp)
        )
    }
}

@Composable
private fun StepIndicator(
    currentIndex: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            val isActive = index == currentIndex
            Box(
                modifier = Modifier
                    .padding(horizontal = 5.dp)
                    .size(if (isActive) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

@Composable
private fun WelcomeStep(
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_onboarding_glasses),
            contentDescription = null,
            modifier = Modifier.size(180.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.onboarding_welcome),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.onboarding_welcome_desc),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = stringResource(R.string.next),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun HowToStep(
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_onboarding_scan),
            contentDescription = null,
            modifier = Modifier.size(160.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.onboarding_howto),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        StepCard(
            number = "1",
            text = stringResource(R.string.onboarding_howto_step1)
        )

        Spacer(modifier = Modifier.height(12.dp))

        StepCard(
            number = "2",
            text = stringResource(R.string.onboarding_howto_step2)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = stringResource(R.string.next),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun StepCard(
    number: String,
    text: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionStep(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeniedMessage by remember { mutableStateOf(false) }
    val backgroundSupported = BackgroundScanSupport.isSupported()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val bluetoothGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions[Manifest.permission.BLUETOOTH_SCAN] == true &&
                permissions[Manifest.permission.BLUETOOTH_CONNECT] == true
        } else {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        }
        if (bluetoothGranted) {
            onComplete()
        } else {
            showDeniedMessage = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Image(
            painter = painterResource(id = R.drawable.ic_onboarding_shield),
            contentDescription = null,
            modifier = Modifier.size(140.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.onboarding_permissions),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        PermissionCard(
            icon = Icons.Rounded.Bluetooth,
            title = stringResource(R.string.onboarding_bluetooth),
            description = stringResource(R.string.onboarding_bluetooth_desc)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            PermissionCard(
                icon = Icons.Rounded.LocationOn,
                title = stringResource(R.string.onboarding_location),
                description = stringResource(R.string.onboarding_location_desc)
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionCard(
                icon = Icons.Rounded.Notifications,
                title = stringResource(R.string.onboarding_notifications),
                description = stringResource(R.string.onboarding_notifications_desc)
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        PermissionCard(
            icon = Icons.Rounded.BatteryFull,
            title = stringResource(R.string.onboarding_background),
            description = stringResource(
                if (backgroundSupported) {
                    R.string.onboarding_background_desc_supported
                } else {
                    R.string.onboarding_background_desc_unsupported
                }
            )
        )

        if (showDeniedMessage) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.onboarding_permission_denied),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    buildList {
                        add(Manifest.permission.BLUETOOTH_SCAN)
                        add(Manifest.permission.BLUETOOTH_CONNECT)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }.toTypedArray()
                } else {
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                }
                permissionLauncher.launch(permissions)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_grant),
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (showDeniedMessage) {
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.material3.TextButton(
                onClick = onComplete
            ) {
                Text(
                    text = stringResource(R.string.onboarding_skip),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
