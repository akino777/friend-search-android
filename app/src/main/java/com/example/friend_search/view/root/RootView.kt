package com.example.friend_search.view.root

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.friend_search.R
import com.example.friend_search.view.authentication.AuthFlowView
import com.example.friend_search.view.profile.ProfileSetupView
import com.example.friend_search.view.shared.MainTabView
import com.example.friend_search.view.shared.launch
import com.example.friend_search.viewmodel.game.GameCatalogStore
import com.example.friend_search.viewmodel.profile.ProfileSetupViewModel
import com.example.friend_search.viewmodel.root.RootViewModel
import com.example.friend_search.viewmodel.shared.userMessage

/** 画面をまたいで共有するゲームマスタ（iOS の Environment に相当） */
val LocalGameCatalog = staticCompositionLocalOf<GameCatalogStore> { error("GameCatalogStore がありません") }

/** ルートの状態（サインアウトなど）を画面から変えるため共有する */
val LocalRootViewModel = staticCompositionLocalOf<RootViewModel> { error("RootViewModel がありません") }

/**
 * ルート画面の種類。状態が変わるたびに別の画面（NavEntry）に置き換え、前の画面の ViewModel は破棄する
 * （サインアウト後に、前のログインのタブや入力が残らないようにする）
 */
private enum class RootScreen { LAUNCHING, SIGNED_OUT, NEEDS_PROFILE, PROFILE_LOAD_FAILED, READY }

@Composable
fun RootView() {
    // Activity の間（画面回転などの構成変更をまたいで）保持する
    val root = viewModel { RootViewModel() }
    val catalog = viewModel { GameCatalogStore() }
    LaunchedEffect(Unit) { root.start() }

    val screen = when (root.state) {
        RootViewModel.State.Launching -> RootScreen.LAUNCHING
        is RootViewModel.State.SignedOut -> RootScreen.SIGNED_OUT
        is RootViewModel.State.NeedsProfile -> RootScreen.NEEDS_PROFILE
        is RootViewModel.State.ProfileLoadFailed -> RootScreen.PROFILE_LOAD_FAILED
        is RootViewModel.State.Ready -> RootScreen.READY
    }

    CompositionLocalProvider(LocalRootViewModel provides root, LocalGameCatalog provides catalog) {
        Surface(Modifier.fillMaxSize()) {
            NavDisplay(
                backStack = listOf(screen),
                entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                popTransitionSpec = { fadeIn() togetherWith fadeOut() },
                entryProvider = { key -> NavEntry(key) { RootContent(root) } },
            )
        }
    }
}

@Composable
private fun RootContent(root: RootViewModel) {
    when (val state = root.state) {
        RootViewModel.State.Launching -> Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Text("読み込み中…", modifier = Modifier.padding(top = 12.dp))
        }
        is RootViewModel.State.SignedOut -> AuthFlowView(initialMessage = state.message) {
            root.launch { root.loadProfile() }
        }
        is RootViewModel.State.NeedsProfile -> ProfileSetupView(viewModel { ProfileSetupViewModel() }, avatar = state.avatar) { profile ->
            root.didCreateProfile(profile)
        }
        is RootViewModel.State.ProfileLoadFailed -> Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(painterResource(R.drawable.wifi_off_24px), contentDescription = null, modifier = Modifier.size(48.dp))
            Text("プロフィールを取得できません", style = MaterialTheme.typography.titleMedium)
            Text(state.error.userMessage, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { root.launch { root.loadProfile() } }) { Text("再試行") }
            TextButton(onClick = { root.launch { root.signOut() } }) {
                Text("ログアウト", color = MaterialTheme.colorScheme.error)
            }
        }
        is RootViewModel.State.Ready -> MainTabView(state.profile)
    }
}
