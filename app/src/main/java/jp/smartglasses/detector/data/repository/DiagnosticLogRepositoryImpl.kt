package jp.smartglasses.detector.data.repository

import jp.smartglasses.detector.data.database.DiagnosticLogDao
import jp.smartglasses.detector.data.database.DiagnosticLogEntity
import jp.smartglasses.detector.domain.model.DiagnosticLog
import jp.smartglasses.detector.domain.repository.DiagnosticLogRepository
import jp.smartglasses.detector.util.Constants
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosticLogRepositoryImpl @Inject constructor(
    private val dao: DiagnosticLogDao
) : DiagnosticLogRepository {
    override suspend fun insertLog(log: DiagnosticLog) {
        dao.insertLog(log.toEntity())
        val overflow = dao.count() - Constants.DIAGNOSTIC_LOG_KEEP_COUNT
        if (overflow > 0) {
            dao.deleteOldest(overflow)
        }
    }

    override suspend fun getLatestLogs(limit: Int): List<DiagnosticLog> {
        return dao.getLatestLogs(limit).map { it.toDomain() }
    }

    private fun DiagnosticLogEntity.toDomain() = DiagnosticLog(
        id = id,
        advertisedName = advertisedName,
        deviceAddress = deviceAddress,
        companyIds = companyIds,
        serviceUuids = serviceUuids,
        advertisementDataHex = advertisementDataHex,
        rssi = rssi,
        detectedAt = detectedAt
    )

    private fun DiagnosticLog.toEntity() = DiagnosticLogEntity(
        id = id,
        advertisedName = advertisedName,
        deviceAddress = deviceAddress,
        companyIds = companyIds,
        serviceUuids = serviceUuids,
        advertisementDataHex = advertisementDataHex,
        rssi = rssi,
        detectedAt = detectedAt
    )
}
