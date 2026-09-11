package jp.smartglasses.detector.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.smartglasses.detector.R
import jp.smartglasses.detector.data.export.DiagnosticLogExporter
import jp.smartglasses.detector.domain.model.DetectionHistoryGrouping
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
import java.io.IOException
import javax.inject.Inject

sealed class HistoryUiState {
    object Loading : HistoryUiState()
    object Empty : HistoryUiState()
    data class Success(val groupedLogs: Map<String, List<DetectionLog>>) : HistoryUiState()
}

sealed interface HistoryEvent {
    data class ShowMessage(val messageResId: Int) : HistoryEvent
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
                HistoryUiState.Success(DetectionHistoryGrouping.groupByDate(logs))
            }
        }
        .catch { emit(HistoryUiState.Empty) }
        .stateIn(viewModelScope, SharingStarted.Lazily, HistoryUiState.Loading)

    fun shareDiagnosticLogs() {
        viewModelScope.launch {
            try {
                val uri = diagnosticLogExporter.exportLatestLogs()
                if (uri == null) {
                    _event.send(HistoryEvent.ShowMessage(R.string.error_diagnostic_empty))
                } else {
                    _event.send(HistoryEvent.ShareDiagnosticLogs(uri))
                }
            } catch (_: IOException) {
                _event.send(HistoryEvent.ShowMessage(R.string.error_diagnostic_create))
            } catch (_: IllegalArgumentException) {
                _event.send(HistoryEvent.ShowMessage(R.string.error_diagnostic_prepare))
            }
        }
    }
}
