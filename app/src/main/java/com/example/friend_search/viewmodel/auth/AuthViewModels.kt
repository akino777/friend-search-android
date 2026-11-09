package com.example.friend_search.viewmodel.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.friend_search.model.AppError
import com.example.friend_search.model.SignInOutcome
import com.example.friend_search.usecase.auth.DefaultSignInUseCase
import com.example.friend_search.usecase.auth.SignInUseCase
import com.example.friend_search.viewmodel.shared.EmailRule
import com.example.friend_search.viewmodel.shared.userMessage
import kotlin.coroutines.cancellation.CancellationException

class SignInViewModel(
    private val signIn: SignInUseCase = DefaultSignInUseCase(),
) : ViewModel() {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)

    val canSubmit: Boolean get() = email.isNotEmpty() && password.isNotEmpty() && !isLoading

    /** ログインできたら true */
    suspend fun submit(): Boolean {
        if (isLoading) return false
        val normalizedEmail = EmailRule.normalized(email) ?: run {
            errorMessage = EmailRule.INVALID_MESSAGE
            return false
        }
        if (password.isEmpty()) {
            errorMessage = "パスワードを入力してください"
            return false
        }
        isLoading = true
        errorMessage = null
        try {
            return when (signIn.execute(normalizedEmail, password)) {
                SignInOutcome.SIGNED_IN -> true
                SignInOutcome.UNSUPPORTED_STEP -> {
                    // MFA などの追加手順があっても成功扱いにしない
                    errorMessage = "このアカウントには追加の認証手順が設定されているため、このアプリではログインできません"
                    false
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorMessage = AppError.from(e).userMessage
            return false
        } finally {
            isLoading = false
        }
    }
}
