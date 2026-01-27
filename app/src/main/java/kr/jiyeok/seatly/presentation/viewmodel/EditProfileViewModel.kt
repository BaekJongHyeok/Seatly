package kr.jiyeok.seatly.presentation.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kr.jiyeok.seatly.data.remote.request.ChangePasswordRequest
import kr.jiyeok.seatly.data.remote.request.UpdateUserInfoRequest
import kr.jiyeok.seatly.data.remote.response.UserInfoSummaryDto
import kr.jiyeok.seatly.data.repository.ApiResult
import kr.jiyeok.seatly.di.IoDispatcher
import kr.jiyeok.seatly.domain.usecase.ChangePasswordUseCase
import kr.jiyeok.seatly.domain.usecase.DeleteAccountUseCase
import kr.jiyeok.seatly.domain.usecase.GetUserInfoUseCase
import kr.jiyeok.seatly.domain.usecase.UpdateUserInfoUseCase
import kr.jiyeok.seatly.domain.usecase.UploadImageUseCase
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.math.min

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val updateUserInfoUseCase: UpdateUserInfoUseCase,
    private val uploadImageUseCase: UploadImageUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    // 사용자 정보
    private val _userData = MutableStateFlow<UserInfoSummaryDto?>(null)
    val userData: StateFlow<UserInfoSummaryDto?> = _userData.asStateFlow()

    // 업데이트 성공
    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess.asStateFlow()

    // 비밀번호 변경
    private val _changePasswordSuccess = MutableStateFlow(false)
    val changePasswordSuccess: StateFlow<Boolean> = _changePasswordSuccess

    // 계정 삭제
    private val _deleteAccountSuccess = MutableStateFlow(false)
    val deleteAccountSuccess: StateFlow<Boolean> = _deleteAccountSuccess.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _events = Channel<String>(Channel.Factory.BUFFERED)
    val events = _events.receiveAsFlow()

    companion object {
        private const val MAX_IMAGE_SIZE = 1024 * 1024 // 1MB
        private const val COMPRESSED_IMAGE_WIDTH = 800 // 최대 가로 크기
        private const val COMPRESSED_IMAGE_HEIGHT = 800 // 최대 세로 크기
    }

    /**
     * 사용자 프로필 조회
     */
    fun loadUserProfile() {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            try {
                when (val result = getUserInfoUseCase()) {
                    is ApiResult.Success -> {
                        _userData.value = result.data
                    }
                    is ApiResult.Failure -> {
                        _userData.value = null
                        _events.send(result.message ?: "사용자 정보 조회 실패")
                    }
                }
            } catch (e: Exception) {
                _events.send(e.message ?: "알 수 없는 오류")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 사용자 프로필 업데이트
     */
    fun updateUserProfile(name: String, phoneNumber: String, imageUri: Uri? = null) {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            try {
                var finalImageId: String? = userData.value?.imageUrl

                // 1. 이미지가 변경되었으면 업로드
                if (imageUri != null) {
                    val uploadedImageId = uploadImage(imageUri)
                    if (uploadedImageId != null) {
                        finalImageId = uploadedImageId
                    } else {
                        _events.send("이미지 업로드 실패")
                        _isLoading.value = false
                        return@launch
                    }
                }

                // 2. 사용자 정보 업데이트
                val updateRequest = UpdateUserInfoRequest(name, phoneNumber, finalImageId)
                when (val result = updateUserInfoUseCase(updateRequest)) {
                    is ApiResult.Success -> {
                        _updateSuccess.value = true
                        _events.send("프로필이 업데이트되었습니다")
                        loadUserProfile()
                    }
                    is ApiResult.Failure -> {
                        _updateSuccess.value = false
                        _events.send(result.message ?: "사용자 정보 업데이트 실패")
                    }
                }
            } catch (e: Exception) {
                _events.send(e.message ?: "알 수 없는 오류")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 이미지 업로드 (압축 포함)
     */
    private suspend fun uploadImage(uri: Uri): String? {
        return try {
            // 1. Uri -> 압축된 File 변환
            val compressedFile = compressImage(uri) ?: run {
                _events.send("이미지 압축 실패")
                return null
            }

            // 2. 파일 크기 체크
            if (compressedFile.length() > MAX_IMAGE_SIZE) {
                compressedFile.delete()
                _events.send("이미지 크기가 너무 큽니다 (최대 1MB)")
                return null
            }

            // 3. File -> MultipartBody.Part
            val requestFile = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", compressedFile.name, requestFile)

            // 4. API 호출
            val result = when (val uploadResult = uploadImageUseCase(body)) {
                is ApiResult.Success -> {
                    uploadResult.data
                }
                is ApiResult.Failure -> {
                    _events.send(uploadResult.message ?: "이미지 업로드 실패")
                    null
                }
            }

            // 5. 임시 파일 삭제
            compressedFile.delete()
            result
        } catch (e: Exception) {
            _events.send(e.message ?: "이미지 처리 실패")
            null
        }
    }

    /**
     * 이미지 압축 (최대 800x800, JPEG 품질 85%)
     */
    private fun compressImage(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null

            // 1. 원본 Bitmap 로드
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) return null

            // 2. EXIF 정보 읽기 (회전 정보)
            val exifRotation = getExifRotation(uri)

            // 3. 리사이징
            val resizedBitmap = resizeBitmap(originalBitmap, COMPRESSED_IMAGE_WIDTH, COMPRESSED_IMAGE_HEIGHT)

            // 4. 회전 적용
            val rotatedBitmap = rotateBitmap(resizedBitmap, exifRotation)

            // 5. 임시 파일로 저장 (JPEG, 품질 85%)
            val tempFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { outputStream ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            }

            // 6. Bitmap 해제
            if (originalBitmap != resizedBitmap) originalBitmap.recycle()
            if (resizedBitmap != rotatedBitmap) resizedBitmap.recycle()
            rotatedBitmap.recycle()

            tempFile
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Bitmap 리사이징 (비율 유지)
     */
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val scale = min(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * EXIF 회전 정보 읽기
     */
    private fun getExifRotation(uri: Uri): Int {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return 0
            val exif = ExifInterface(inputStream)
            inputStream.close()

            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Bitmap 회전
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap

        val matrix = Matrix().apply {
            postRotate(degrees.toFloat())
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * 비밀번호 변경
     */
    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val changeRequest = ChangePasswordRequest(currentPassword, newPassword)
                when (val result = changePasswordUseCase(changeRequest)) {
                    is ApiResult.Success -> {
                        _changePasswordSuccess.value = true
                        _events.send("비밀번호가 변경되었습니다")
                    }
                    is ApiResult.Failure -> {
                        _changePasswordSuccess.value = false
                        _events.send(result.message ?: "비밀번호 변경 실패")
                    }
                }
            } catch (e: Exception) {
                _events.send(e.message ?: "알 수 없는 오류")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 계정 탈퇴 요청
     */
    fun deleteAccount() {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            try {
                when (val result = deleteAccountUseCase()) {
                    is ApiResult.Success -> {
                        _deleteAccountSuccess.value = true
                        _events.send("계정이 탈퇴되었습니다")
                    }
                    is ApiResult.Failure -> {
                        _deleteAccountSuccess.value = false
                        _events.send(result.message ?: "계정 탈퇴 실패")
                    }
                }
            } catch (e: Exception) {
                _events.send(e.message ?: "알 수 없는 오류")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetUpdateSuccess() {
        _updateSuccess.value = false
    }
}
