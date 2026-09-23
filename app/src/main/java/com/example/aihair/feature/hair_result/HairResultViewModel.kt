package com.example.aihair.feature.hair_result

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aihair.R
import com.example.aihair.core.data.remote.dto.generation.GenerationState
import com.example.aihair.core.data.repository.GenerationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import java.io.File
import android.net.Uri
import com.example.aihair.feature.main.history.HistoryItem
import com.example.aihair.feature.main.history.data.HistoryRepository
import com.example.aihair.core.utils.saveImageToGallery

@HiltViewModel
class HairResultViewModel @Inject constructor(
    private val generationRepository: GenerationRepository,
    private val historyRepository: HistoryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<HairResultUiState>(HairResultUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var currentLocalImageUri: String? = null
    private var currentOriginalImageUri: String? = null
    private var currentHistoryType: Int = 0
    private var currentStyleName: String? = null
    private var currentIsColorMode: Boolean = false
    private var currentIsFemale: Boolean = true

    private val _event = MutableSharedFlow<HairResultEvent>()
    val event = _event.asSharedFlow()

    fun onAction(action: HairResultAction) {
        when (action) {
            is HairResultAction.LoadData -> handleLoadData(action)
            HairResultAction.ShareClicked -> handleShareClicked()
            HairResultAction.SaveClicked -> handleSaveClicked()
            HairResultAction.HomeClicked -> handleHomeClicked()
            HairResultAction.BackClicked -> handleBackClicked()
            HairResultAction.CreateAgainClicked -> handleCreateAgainClicked()
        }
    }

    private fun handleShareClicked() {
        val currentState = _uiState.value
        if (currentState is HairResultUiState.Success) {
            currentState.imageUrl?.let { url ->
                viewModelScope.launch {
                    _event.emit(HairResultEvent.ShareImage(url))
                }
            }
        }
    }

    private fun handleSaveClicked() {
        val currentState = _uiState.value
        if (currentState is HairResultUiState.Success) {
            currentState.imageUrl?.let { url ->
                viewModelScope.launch {
                    val success = context.saveImageToGallery(url)
                    if (success) {
                        _event.emit(HairResultEvent.ShowToast(context.getString(R.string.msg_image_saved_to_gallery)))
                    } else {
                        _event.emit(HairResultEvent.ShowToast(context.getString(R.string.msg_error_saving_image)))
                    }
                }
            }
        }
    }

    private fun handleHomeClicked() {
        viewModelScope.launch {
            _event.emit(HairResultEvent.NavigateToHome)
        }
    }

    private fun handleBackClicked() {
        viewModelScope.launch {
            _event.emit(HairResultEvent.NavigateBack)
        }
    }

    private fun handleCreateAgainClicked() {
        val uri = currentLocalImageUri ?: return
        _uiState.value = HairResultUiState.Loading
        startGenerationProcess(uri, currentOriginalImageUri, currentHistoryType, currentStyleName, currentIsColorMode, currentIsFemale)
    }

    private fun handleLoadData(action: HairResultAction.LoadData) {
        currentLocalImageUri = action.localImageUri
        currentOriginalImageUri = action.originalImageUri
        currentHistoryType = action.historyType
        currentStyleName = action.styleName
        currentIsColorMode = action.isColorMode
        currentIsFemale = action.isFemale

        if (action.historyType == 1) {
            // Từ màn PhotoEditorActivity (chỉnh sửa thủ công), đã có ảnh kết quả
            _uiState.value = HairResultUiState.Success(action.localImageUri)
            if (action.localImageUri != null) {
                saveToHistory(action.originalImageUri, action.localImageUri, action.historyType)
            }
        } else {
            // Từ màn HairAIActivity, cần gọi API tạo ảnh
            if (action.localImageUri == null) {
                _uiState.value = HairResultUiState.Error(context.getString(R.string.msg_error_no_image_to_process))
                return
            }
            startGenerationProcess(action.localImageUri, action.originalImageUri, action.historyType, action.styleName, action.isColorMode, action.isFemale)
        }
    }

    private fun startGenerationProcess(localImageUri: String, originalImageUri: String?, historyType: Int, styleName: String?, isColorMode: Boolean, isFemale: Boolean) {
        viewModelScope.launch {
            generationRepository.startGeneration(localImageUri, styleName, isColorMode, isFemale).collect { state ->
                when (state) {
                    is GenerationState.Uploading,
                    is GenerationState.Submitting,
                    is GenerationState.Processing -> {
                        _uiState.value = HairResultUiState.Loading
                    }
                    is GenerationState.Success -> {
                        _uiState.value = HairResultUiState.Success(state.imageUrl)
                        saveToHistory(originalImageUri, state.imageUrl, historyType)
                    }
                    is GenerationState.Error -> {
                        val errorMessage = when (val error = state.error) {
                            is GenerationState.GenerationError.StorageError -> context.getString(R.string.msg_error_upload_image, error.message)
                            is GenerationState.GenerationError.SubmitError -> context.getString(R.string.msg_error_create_request, error.code, error.message)
                            is GenerationState.GenerationError.PollingError -> context.getString(R.string.msg_error_read_result, error.code, error.message)
                            is GenerationState.GenerationError.TaskFailed -> context.getString(R.string.msg_error_task_failed, error.message)
                            is GenerationState.GenerationError.NetworkError -> context.getString(R.string.msg_error_network, error.message)
                            is GenerationState.GenerationError.TimeoutError -> context.getString(R.string.msg_error_timeout)
                        }
                        _uiState.value = HairResultUiState.Error(errorMessage)
                    }
                }
            }
        }
    }

    private fun saveToHistory(originalImageUriString: String?, resultImageUri: String, historyType: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            if (originalImageUriString != null) {
                val safeOriginalUri = copyImageToInternalStorage(originalImageUriString) ?: originalImageUriString
                val item = HistoryItem(
                    originalImageUri = safeOriginalUri,
                    resultImageUri = resultImageUri,
                    type = historyType
                )
                historyRepository.addHistoryItem(item)
            }
        }
    }

    private fun copyImageToInternalStorage(uriStr: String): String? {
        if (!uriStr.startsWith("content://")) return uriStr
        return try {
            val uri = Uri.parse(uriStr)
            val file = File(context.filesDir, "history_orig_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
