import CoreBluetooth
import Foundation

enum AdvertisementAssembler {
    static func hex(from advertisementData: [String: Any]) -> String {
        var bytes = Data()
        appendLocalName(&bytes, advertisementData)
        appendManufacturer(&bytes, advertisementData)
        appendServiceUUIDs(
            &bytes,
            advertisementData[CBAdvertisementDataServiceUUIDsKey] as? [CBUUID],
            type16: 0x03,
            type32: 0x05,
            type128: 0x07
        )
        appendServiceUUIDs(
            &bytes,
            advertisementData[CBAdvertisementDataOverflowServiceUUIDsKey] as? [CBUUID],
            type16: 0x03,
            type32: 0x05,
            type128: 0x07
        )
        appendServiceUUIDs(
            &bytes,
            advertisementData[CBAdvertisementDataSolicitedServiceUUIDsKey] as? [CBUUID],
            type16: 0x14,
            type32: 0x1F,
            type128: 0x15
        )
        appendServiceData(&bytes, advertisementData)
        return hexString(bytes)
    }

    static func companyIdsCsv(from advertisementData: [String: Any]) -> String {
        guard let manufacturer = advertisementData[CBAdvertisementDataManufacturerDataKey] as? Data,
              manufacturer.count >= 2 else {
            return ""
        }
        let companyId = Int(manufacturer[0]) | (Int(manufacturer[1]) << 8)
        return String(companyId)
    }

    static func serviceUuidsCsv(from advertisementData: [String: Any], extra: [CBUUID] = []) -> String {
        var uuids: [String] = []
        let groups: [[CBUUID]] = [
            advertisementData[CBAdvertisementDataServiceUUIDsKey] as? [CBUUID] ?? [],
            advertisementData[CBAdvertisementDataOverflowServiceUUIDsKey] as? [CBUUID] ?? [],
            advertisementData[CBAdvertisementDataSolicitedServiceUUIDsKey] as? [CBUUID] ?? extra,
            extra
        ]
        if let serviceData = advertisementData[CBAdvertisementDataServiceDataKey] as? [CBUUID: Data] {
            uuids.append(contentsOf: serviceData.keys.map(\.uuidString))
        }
        for group in groups {
            uuids.append(contentsOf: group.map(\.uuidString))
        }
        return Array(NSOrderedSet(array: uuids)).compactMap { $0 as? String }.joined(separator: ",")
    }

    static func localName(from advertisementData: [String: Any], peripheralName: String?) -> String? {
        let advertised = advertisementData[CBAdvertisementDataLocalNameKey] as? String
        return firstUsableName(advertised, peripheralName)
    }

    static func firstUsableName(_ names: String?...) -> String? {
        let ignored: Set<String> = ["unknown", "n/a", "null"]
        for name in names {
            let trimmed = name?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            if trimmed.isEmpty {
                continue
            }
            if ignored.contains(trimmed.lowercased()) {
                continue
            }
            return trimmed
        }
        return nil
    }

    private static func appendLocalName(_ bytes: inout Data, _ advertisementData: [String: Any]) {
        guard let name = advertisementData[CBAdvertisementDataLocalNameKey] as? String else {
            return
        }
        let nameBytes = Data(name.utf8).prefix(254)
        guard !nameBytes.isEmpty else {
            return
        }
        bytes.append(UInt8(nameBytes.count + 1))
        bytes.append(0x09)
        bytes.append(nameBytes)
    }

    private static func appendManufacturer(_ bytes: inout Data, _ advertisementData: [String: Any]) {
        guard let manufacturer = advertisementData[CBAdvertisementDataManufacturerDataKey] as? Data else {
            return
        }
        let payload = manufacturer.prefix(253)
        bytes.append(UInt8(payload.count + 1))
        bytes.append(0xFF)
        bytes.append(payload)
    }

    private static func appendServiceUUIDs(
        _ bytes: inout Data,
        _ uuids: [CBUUID]?,
        type16: UInt8,
        type32: UInt8,
        type128: UInt8
    ) {
        guard let uuids, !uuids.isEmpty else {
            return
        }
        for uuid in uuids {
            let data = uuid.data
            let type: UInt8
            switch data.count {
            case 2:
                type = type16
            case 4:
                type = type32
            case 16:
                type = type128
            default:
                continue
            }
            bytes.append(UInt8(data.count + 1))
            bytes.append(type)
            bytes.append(data)
        }
    }

    private static func appendServiceData(_ bytes: inout Data, _ advertisementData: [String: Any]) {
        guard let serviceData = advertisementData[CBAdvertisementDataServiceDataKey] as? [CBUUID: Data] else {
            return
        }
        for (uuid, payload) in serviceData {
            let uuidBytes = uuid.data
            let type: UInt8
            switch uuidBytes.count {
            case 2:
                type = 0x16
            case 4:
                type = 0x20
            case 16:
                type = 0x21
            default:
                continue
            }
            let body = (uuidBytes + payload).prefix(254)
            bytes.append(UInt8(body.count + 1))
            bytes.append(type)
            bytes.append(body)
        }
    }

    private static func hexString(_ data: Data) -> String {
        data.map { String(format: "%02X", $0) }.joined()
    }
}
