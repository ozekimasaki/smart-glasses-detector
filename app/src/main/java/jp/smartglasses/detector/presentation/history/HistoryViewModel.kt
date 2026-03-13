package jp.smartglasses.detector.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.smartglasses.detector.data.export.DiagnosticLogExporter
import jp.smartglasses.detector.domain.model.DetectionLog
import jp.smartglasses.detector.domain.usecase.GetDetectionHistoryUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

sealed class HistoryUiState {
    object Loading : HistoryUiState()
    object Empty : HistoryUiState()
    data class Success(val groupedLogs: Map<String, List<DetectionLog>>) : HistoryUiState()
}

sealed interface HistoryEvent {
    data class ShowMessage(val message: String) : HistoryEvent
    data class ShareDiagnosticLogs(val uri: Uri) : HistoryEvent
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    getDetectionHistoryUseCase: GetDetectionHistoryUseCase,
    private val diagnosticLogExporter: DiagnosticLogExporter
) : ViewModel() {
    private val _event = Channel<HistoryEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()

    val uiState = getDetectionHistoryUseCase()
        .map { logs ->
            if (logs.isEmpty()) {
                HistoryUiState.Empty
            } else {
                HistoryUiState.Success(groupLogsByDate(logs))
            }
        }
        .catch { emit(HistoryUiState.Empty) }
        .stateIn(viewModelScope, SharingStarted.Lazily, HistoryUiState.Loading)

    fun shareDiagnosticLogs() {
        viewModelScope.launch {
            val uri = diagnosticLogExporter.exportLatestLogs()
            if (uri == null) {
                _event.send(HistoryEvent.ShowMessage("共有できる調査ログがまだありません。"))
            } else {
                _event.send(HistoryEvent.ShareDiagnosticLogs(uri))
            }
        }
    }

    private fun groupLogsByDate(logs: List<DetectionLog>): Map<String, List<DetectionLog>> {
        val today = getStartOfDay(System.currentTimeMillis())
        val yesterday = getStartOfDay(System.currentTimeMillis() - 24 * 60 * 60 * 1000)

        return logs.groupBy { log ->
            val logDay = getStartOfDay(log.detectedAt)
            when {
                logDay == today -> "今日"
                logDay == yesterday -> "昨日"
                else -> formatDate(log.detectedAt)
            }
        }
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("M月d日", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
