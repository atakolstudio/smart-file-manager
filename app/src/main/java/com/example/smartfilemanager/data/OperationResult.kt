package com.example.smartfilemanager.data

/**
 * Dosya işlemlerinin sonucunu güvenli biçimde taşıyan sarmalayıcı.
 * Böylece her işlem try-catch içinde yürütülür ve çağıran taraf çökme riski olmadan
 * kullanıcı dostu bir hata mesajı gösterebilir.
 */
sealed class OperationResult<out T> {
    data class Success<out T>(val data: T) : OperationResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : OperationResult<Nothing>()
}

inline fun <T> safeFileOperation(errorMessage: String, block: () -> T): OperationResult<T> {
    return try {
        OperationResult.Success(block())
    } catch (t: Throwable) {
        android.util.Log.e("SmartFileManager", errorMessage, t)
        OperationResult.Error(t.message ?: errorMessage, t)
    }
}
