package com.example.friend_search.viewmodel

import com.example.friend_search.model.AppError
import com.example.friend_search.model.AuthFailure
import com.example.friend_search.model.SignInOutcome
import com.example.friend_search.viewmodel.auth.SignInViewModel
import com.example.friend_search.viewmodel.shared.EmailRule
import com.example.friend_search.viewmodel.shared.userMessage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// 認証画面の ViewModel: 入力検証と結果に応じた遷移。依存は UseCase のモックのみ。

class SignInViewModelTests {
    private val signIn = MockSignInUseCase()

    private fun make(email: String = " User@Example.com ", password: String = "Passw0rd") =
        SignInViewModel(signIn).also {
            it.email = email
            it.password = password
        }

    @Test
    fun メールアドレスを正規化してサインインする() = runTest {
        signIn.handler = { _, _ -> SignInOutcome.SIGNED_IN }
        assertTrue(make().submit())
        assertEquals(listOf("user@example.com"), signIn.emails)
    }

    @Test
    fun 形式が不正なメールアドレスは送らない() = runTest {
        val viewModel = make(email = "user@")
        assertFalse(viewModel.submit())
        assertEquals(EmailRule.INVALID_MESSAGE, viewModel.errorMessage)
        assertTrue(signIn.emails.isEmpty())
    }

    @Test
    fun 対応しない追加手順は成功扱いにしない() = runTest {
        signIn.handler = { _, _ -> SignInOutcome.UNSUPPORTED_STEP }
        val viewModel = make()
        assertFalse(viewModel.submit())
        assertNotNull(viewModel.errorMessage)
    }

    @Test
    fun 認証エラーを表示する() = runTest {
        signIn.handler = { _, _ -> throw AppError.Auth(AuthFailure.InvalidCredentials) }
        val viewModel = make()
        assertFalse(viewModel.submit())
        assertEquals(AuthFailure.InvalidCredentials.userMessage, viewModel.errorMessage)
    }
}
