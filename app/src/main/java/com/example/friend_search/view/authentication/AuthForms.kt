package com.example.friend_search.view.authentication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.friend_search.R
import com.example.friend_search.view.shared.FormMessage
import com.example.friend_search.view.shared.ScreenScaffold
import com.example.friend_search.view.shared.SubmitButton
import com.example.friend_search.view.shared.launch
import com.example.friend_search.viewmodel.auth.SignInViewModel

/** フォームの縦並び（スクロール・キーボード分の余白つき） */
@Composable
private fun FormColumn(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}

@Composable
private fun EmailField(value: String, onValueChange: (String) -> Unit, tag: String? = null) {
    OutlinedTextField(
        value, onValueChange,
        label = { Text("メールアドレス") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrectEnabled = false),
        modifier = Modifier.fillMaxWidth().then(if (tag != null) Modifier.testTag(tag) else Modifier),
    )
}

@Composable
private fun PasswordField(label: String, value: String, onValueChange: (String) -> Unit, tag: String? = null) {
    OutlinedTextField(
        value, onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth().then(if (tag != null) Modifier.testTag(tag) else Modifier),
    )
}

/** A03 ログイン */
@Composable
fun SignInView(viewModel: SignInViewModel, message: String?, onSignedIn: () -> Unit) {
    ScreenScaffold(title = "ログイン") { modifier ->
        FormColumn(modifier) {
            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(painterResource(R.drawable.sports_esports_24px), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
                Text("friend-search", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("一緒に遊ぶ仲間をゲームごとに探そう", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (message != null) FormMessage(message, isError = false)
            EmailField(viewModel.email, { viewModel.email = it }, tag = "signIn.email")
            PasswordField("パスワード", viewModel.password, { viewModel.password = it }, tag = "signIn.password")
            viewModel.errorMessage?.let { FormMessage(it) }
            SubmitButton("ログイン", viewModel.isLoading, viewModel.canSubmit, Modifier.testTag("signIn.submit")) {
                viewModel.launch { if (viewModel.submit()) onSignedIn() }
            }
        }
    }
}
