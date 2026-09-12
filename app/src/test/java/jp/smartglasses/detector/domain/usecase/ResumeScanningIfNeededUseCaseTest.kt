package jp.smartglasses.detector.domain.usecase

import jp.smartglasses.detector.domain.model.BluetoothScanFailure
import jp.smartglasses.detector.domain.model.SmartGlassesDevice
import jp.smartglasses.detector.domain.repository.BluetoothRepository
import jp.smartglasses.detector.domain.repository.SettingsRepository
import jp.smartglasses.detector.domain.service.ScanServiceController
import jp.smartglasses.detector.util.ScanSensitivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ResumeScanningIfNeededUseCaseTest {
    @Test
    fun `starts the scan service when the app is visible and scanning was persisted`() = runBlocking {
        val controller = RecordingScanServiceController()
        val useCase = ResumeScanningIfNeededUseCase(
            settingsRepository = FakeSettingsRepository(scanning = true, background = false),
            bluetoothRepository = FakeBluetoothRepository(permissions = true),
            scanServiceController = controller
        )

        useCase(appInForeground = true)

        assertEquals(1, controller.startCount)
        assertEquals(false, controller.lastFromBackground)
    }

    @Test
    fun `does not start from background when background scanning is unavailable`() = runBlocking {
        val controller = RecordingScanServiceController()
        val useCase = ResumeScanningIfNeededUseCase(
            settingsRepository = FakeSettingsRepository(scanning = true, background = false),
            bluetoothRepository = FakeBluetoothRepository(permissions = true),
            scanServiceController = controller
        )

        useCase(appInForeground = false)

        assertEquals(0, controller.startCount)
        assertEquals(null, controller.lastFromBackground)
    }

    @Test
    fun `background resume asks the controller to defer a restricted start`() = runBlocking {
        val controller = RecordingScanServiceController()
        val useCase = ResumeScanningIfNeededUseCase(
            settingsRepository = FakeSettingsRepository(scanning = true, background = true),
            bluetoothRepository = FakeBluetoothRepository(permissions = true),
            scanServiceController = controller
        )

        useCase(appInForeground = false, backgroundScanSupported = true)

        assertEquals(1, controller.startCount)
        assertEquals(true, controller.lastFromBackground)
    }

    @Test
    fun `does not start when the user is not scanning`() = runBlocking {
        val controller = RecordingScanServiceController()
        val useCase = ResumeScanningIfNeededUseCase(
            settingsRepository = FakeSettingsRepository(scanning = false, background = true),
            bluetoothRepository = FakeBluetoothRepository(permissions = true),
            scanServiceController = controller
        )

        useCase(appInForeground = true)

        assertEquals(0, controller.startCount)
    }

    @Test
    fun `does not restart the service when hardware is already scanning`() = runBlocking {
        val controller = RecordingScanServiceController()
        val useCase = ResumeScanningIfNeededUseCase(
            settingsRepository = FakeSettingsRepository(scanning = true, background = false),
            bluetoothRepository = FakeBluetoothRepository(permissions = true, hardwareScanning = true),
            scanServiceController = controller
        )

        useCase(appInForeground = true)

        assertEquals(0, controller.startCount)
    }

    @Test
    fun `restarts the service when session intent is live but hardware is dead`() = runBlocking {
        val controller = RecordingScanServiceController()
        val useCase = ResumeScanningIfNeededUseCase(
            settingsRepository = FakeSettingsRepository(scanning = true, background = false),
            bluetoothRepository = FakeBluetoothRepository(
                permissions = true,
                hardwareScanning = false,
                sessionScanning = true
            ),
            scanServiceController = controller
        )

        useCase(appInForeground = true)

        assertEquals(1, controller.startCount)
    }

    @Test
    fun `does not start when scan permission is missing`() = runBlocking {
        val controller = RecordingScanServiceController()
        val useCase = ResumeScanningIfNeededUseCase(
            settingsRepository = FakeSettingsRepository(scanning = true, background = true),
            bluetoothRepository = FakeBluetoothRepository(permissions = false),
            scanServiceController = controller
        )

        useCase(appInForeground = true)

        assertEquals(0, controller.startCount)
    }

    @Test
    fun `does not start when bluetooth is off`() = runBlocking {
        val controller = RecordingScanServiceController()
        val useCase = ResumeScanningIfNeededUseCase(
            settingsRepository = FakeSettingsRepository(scanning = true, background = true),
            bluetoothRepository = FakeBluetoothRepository(
                permissions = true,
                bluetoothEnabled = false
            ),
            scanServiceController = controller
        )

        useCase(appInForeground = true)

        assertEquals(0, controller.startCount)
    }

    @Test
    fun `does not start when location services are off`() = runBlocking {
        val controller = RecordingScanServiceController()
        val useCase = ResumeScanningIfNeededUseCase(
            settingsRepository = FakeSettingsRepository(scanning = true, background = true),
            bluetoothRepository = FakeBluetoothRepository(
                permissions = true,
                locationEnabled = false
            ),
            scanServiceController = controller
        )

        useCase(appInForeground = true)

        assertEquals(0, controller.startCount)
    }

    private class RecordingScanServiceController : ScanServiceController {
        var startCount = 0
        var lastFromBackground: Boolean? = null

        override fun startScanService(fromBackground: Boolean) {
            startCount += 1
            lastFromBackground = fromBackground
        }

        override fun stopScanService() = Unit
    }

    private class FakeSettingsRepository(
        scanning: Boolean,
        background: Boolean
    ) : SettingsRepository {
        override val backgroundEnabled = MutableStateFlow(background)
        override val notificationEnabled = MutableStateFlow(true)
        override val vibrationEnabled = MutableStateFlow(true)
        override val soundEnabled = MutableStateFlow(true)
        override val sensitivity = MutableStateFlow(ScanSensitivity.BALANCED)
        override val onboardingCompleted = MutableStateFlow(true)
        override val isScanning = MutableStateFlow(scanning)

        override suspend fun setBackgroundEnabled(enabled: Boolean) = Unit
        override suspend fun setNotificationEnabled(enabled: Boolean) = Unit
        override suspend fun setVibrationEnabled(enabled: Boolean) = Unit
        override suspend fun setSoundEnabled(enabled: Boolean) = Unit
        override suspend fun setSensitivity(sensitivity: ScanSensitivity) = Unit
        override suspend fun setOnboardingCompleted(completed: Boolean) = Unit
        override suspend fun setIsScanning(scanning: Boolean) = Unit
    }

    private class FakeBluetoothRepository(
        private val permissions: Boolean,
        hardwareScanning: Boolean = false,
        sessionScanning: Boolean = false,
        private val bluetoothEnabled: Boolean = true,
        private val locationEnabled: Boolean = true
    ) : BluetoothRepository {
        override val scannedDevices: Flow<SmartGlassesDevice> = emptyFlow()
        override val scanFailures: Flow<BluetoothScanFailure> = emptyFlow()
        override val isScanning = MutableStateFlow(sessionScanning)
        override val isHardwareScanRunning = MutableStateFlow(hardwareScanning)
        override val nearbyDevices = MutableStateFlow(emptyList<SmartGlassesDevice>())

        override suspend fun startScanning() = Unit
        override fun ensureHardwareScanning() = Unit
        override fun pauseHardwareKeepingSession() = Unit
        override suspend fun stopScanning() = Unit
        override fun updateScanSensitivity(sensitivity: ScanSensitivity) = Unit
        override fun hasPermissions() = permissions
        override fun hasNotificationPermission() = true
        override fun hasBleHardwareSupport() = true
        override fun isBluetoothEnabled() = bluetoothEnabled
        override fun isLocationServicesEnabled() = locationEnabled
    }
}
