package com.example.friend_search.view.authentication

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.friend_search.viewmodel.auth.SignInViewModel

/** 未ログイン時の画面。ログインは用意済みのアカウントで行う */
@Composable
fun AuthFlowView(initialMessage: String?, onSignedIn: () -> Unit) {
    SignInView(viewModel { SignInViewModel() }, message = initialMessage, onSignedIn = onSignedIn)
}
