import CoreBluetooth
import Foundation
import Shared

final class GlassesScanner: NSObject, CBCentralManagerDelegate {
    static let restoreIdentifier = "jp.smartglasses.detector.central"
    static let unknownRssi = -127

    private let settings: AppSettings
    private let cooldown = DetectionCooldown()
    private let nearby = NearbyTracker()
    private let queue = DispatchQueue(label: "jp.smartglasses.detector.ble")
    private var central: CBCentralManager?
    private var wantsScanning = false
    private var isForeground = true
    private var connectedPoll: DispatchSourceTimer?

    var onState: ((CBManagerState) -> Void)?
    var onNearby: (([DetectedGlasses]) -> Void)?
    var onDetection: ((DetectedGlasses, Bool) -> Void)?

    init(settings: AppSettings) {
        self.settings = settings
        super.init()
    }

    func prepare() {
        queue.async { [weak self] in
            self?.ensureCentral()
        }
    }

    func setForeground(_ foreground: Bool) {
        queue.async { [weak self] in
            guard let self else { return }
            self.isForeground = foreground
            if self.wantsScanning {
                self.applyScanMode()
            }
        }
    }

    func start() {
        queue.async { [weak self] in
            guard let self else { return }
            self.wantsScanning = true
            self.ensureCentral()
            self.applyScanMode()
            self.startConnectedPoll()
        }
    }

    func stop() {
        queue.async { [weak self] in
            guard let self else { return }
            self.wantsScanning = false
            self.connectedPoll?.cancel()
            self.connectedPoll = nil
            self.central?.stopScan()
            self.cooldown.clear()
            self.nearby.clear()
            self.publishNearby()
        }
    }

    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        let state = central.state
        DispatchQueue.main.async { [weak self] in
            self?.onState?(state)
        }
        if state == .poweredOn, wantsScanning {
            applyScanMode()
            pollConnectedPeripherals()
        }
    }

    func centralManager(_ central: CBCentralManager, willRestoreState dict: [String: Any]) {
        if settings.isScanning {
            wantsScanning = true
        }
    }

    func centralManager(
        _ central: CBCentralManager,
        didDiscover peripheral: CBPeripheral,
        advertisementData: [String: Any],
        rssi RSSI: NSNumber
    ) {
        classify(
            address: peripheral.identifier.uuidString,
            rssi: RSSI.intValue,
            name: AdvertisementAssembler.localName(
                from: advertisementData,
                peripheralName: peripheral.name
            ),
            advertisementData: advertisementData,
            extraServices: []
        )
    }

    private func ensureCentral() {
        if central == nil {
            central = CBCentralManager(
                delegate: self,
                queue: queue,
                options: [
                    CBCentralManagerOptionRestoreIdentifierKey: Self.restoreIdentifier,
                    CBCentralManagerOptionShowPowerAlertKey: true
                ]
            )
        }
    }

    private func applyScanMode() {
        guard let central, central.state == .poweredOn, wantsScanning else {
            return
        }
        central.stopScan()
        let services = backgroundServiceUUIDs()
        let useFilter = !isForeground && settings.backgroundEnabled
        central.scanForPeripherals(
            withServices: useFilter ? services : nil,
            options: [CBCentralManagerScanOptionAllowDuplicatesKey: true]
        )
        pollConnectedPeripherals()
    }

    private func startConnectedPoll() {
        connectedPoll?.cancel()
        let timer = DispatchSource.makeTimerSource(queue: queue)
        timer.schedule(deadline: .now() + 3, repeating: 15)
        timer.setEventHandler { [weak self] in
            self?.pollConnectedPeripherals()
            self?.publishNearby()
        }
        timer.resume()
        connectedPoll = timer
    }

    private func pollConnectedPeripherals() {
        guard let central, central.state == .poweredOn, wantsScanning else {
            return
        }
        let services = backgroundServiceUUIDs()
        guard !services.isEmpty else {
            return
        }
        for peripheral in central.retrieveConnectedPeripherals(withServices: services) {
            let extra = peripheral.services?.map(\.uuid) ?? services
            classify(
                address: peripheral.identifier.uuidString,
                rssi: Self.unknownRssi,
                name: AdvertisementAssembler.firstUsableName(peripheral.name),
                advertisementData: [:],
                extraServices: extra
            )
        }
    }

    private func backgroundServiceUUIDs() -> [CBUUID] {
        SmartGlassesDetection.shared.catalogServiceUuidsCsv()
            .split(separator: ",")
            .map { CBUUID(string: String($0)) }
    }

    private func classify(
        address: String,
        rssi: Int,
        name: String?,
        advertisementData: [String: Any],
        extraServices: [CBUUID]
    ) {
        let hex = AdvertisementAssembler.hex(from: advertisementData)
        let companyCsv = AdvertisementAssembler.companyIdsCsv(from: advertisementData)
        let uuidCsv = AdvertisementAssembler.serviceUuidsCsv(
            from: advertisementData,
            extra: extraServices
        )
        let classified = SmartGlassesDetection.shared.classifyAdvertisement(
            address: address,
            rssi: Int32(rssi),
            deviceName: name,
            advertisementHex: hex,
            extraCompanyIdsCsv: companyCsv,
            extraServiceUuidsCsv: uuidCsv,
            appearance: Int32.min,
            deviceClass: Int32.min,
            sensitivityName: settings.sensitivity.kotlinName
        )
        guard let classified else {
            return
        }
        let device = DetectedGlasses(
            id: classified.address,
            name: classified.name,
            manufacturerName: classified.manufacturer.name,
            rssi: Int(classified.rssi),
            distanceLabel: classified.distance.label,
            detectedAt: Date(timeIntervalSince1970: TimeInterval(classified.detectedAt) / 1000)
        )
        nearby.upsert(device)
        let shouldNotify = cooldown.shouldEmit(
            deviceKey: "address:\(device.id)",
            manufacturerKey: device.manufacturerName
        )
        let snapshot = nearby.snapshot()
        DispatchQueue.main.async { [weak self] in
            self?.onDetection?(device, shouldNotify)
            self?.onNearby?(snapshot)
        }
    }

    private func publishNearby() {
        let snapshot = nearby.snapshot()
        DispatchQueue.main.async { [weak self] in
            self?.onNearby?(snapshot)
        }
    }
}
