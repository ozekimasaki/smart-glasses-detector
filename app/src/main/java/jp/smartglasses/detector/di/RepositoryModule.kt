package jp.smartglasses.detector.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jp.smartglasses.detector.data.bluetooth.BluetoothRepositoryImpl
import jp.smartglasses.detector.data.repository.DiagnosticLogRepositoryImpl
import jp.smartglasses.detector.data.repository.DetectionLogRepositoryImpl
import jp.smartglasses.detector.data.repository.SettingsRepositoryImpl
import jp.smartglasses.detector.domain.repository.BluetoothRepository
import jp.smartglasses.detector.domain.repository.DiagnosticLogRepository
import jp.smartglasses.detector.domain.repository.DetectionLogRepository
import jp.smartglasses.detector.domain.repository.SettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindBluetoothRepository(
        impl: BluetoothRepositoryImpl
    ): BluetoothRepository
    
    @Binds
    @Singleton
    abstract fun bindDetectionLogRepository(
        impl: DetectionLogRepositoryImpl
    ): DetectionLogRepository

    @Binds
    @Singleton
    abstract fun bindDiagnosticLogRepository(
        impl: DiagnosticLogRepositoryImpl
    ): DiagnosticLogRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository
}
