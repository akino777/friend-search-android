package com.example.friend_search.view.shared

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.friend_search.R
import com.example.friend_search.model.Post
import com.example.friend_search.model.PublicProfile
import com.example.friend_search.view.discovery.PostListView
import com.example.friend_search.view.post.CreatePostView
import com.example.friend_search.view.post.PostDetailView
import com.example.friend_search.view.profile.MyPageView
import com.example.friend_search.view.root.LocalGameCatalog
import com.example.friend_search.view.root.LocalRootViewModel
import com.example.friend_search.viewmodel.post.CreatePostViewModel
import com.example.friend_search.viewmodel.post.PostDetailViewModel
import com.example.friend_search.viewmodel.post.PostListViewModel
import com.example.friend_search.viewmodel.profile.MyPageViewModel
import java.util.UUID

/** 探す・投稿・マイページのタブ（それぞれのタブの最初の画面のキーも兼ねる） */
enum class MainTab(val label: String, @param:DrawableRes val icon: Int) {
    SEARCH("探す", R.drawable.search_24px),
    POST("投稿", R.drawable.edit_square_24px),
    MY_PAGE("マイページ", R.drawable.account_circle_24px),
}

/** 募集詳細へのナビゲーション先。onUpdate で詳細での変更を呼び出し元（AI 検索の結果）へ伝える */
class PostDetailRoute(val post: Post, val onUpdate: (Post) -> Unit = {}) {
    /** 同じ募集を開き直しても別の画面として扱う */
    val id: String = UUID.randomUUID().toString()
}

/**
 * タブごとの back stack（View の状態）。画面回転などの構成変更で失わないよう ViewModel に持つ。
 * キーは Post や関数を含むため保存（プロセス終了からの復元）はしない。
 */
class MainNavigationState : ViewModel() {
    var selectedTab by mutableStateOf(MainTab.SEARCH)
    val backStacks: Map<MainTab, SnapshotStateList<Any>> = MainTab.entries.associateWith { mutableStateListOf<Any>(it) }

    fun push(key: Any) {
        backStacks.getValue(selectedTab).add(key)
    }

    /** タブの最初の画面で戻ると「探す」タブへ戻る（「探す」の最初の画面ではアプリを閉じる） */
    fun goBack() {
        val stack = backStacks.getValue(selectedTab)
        if (stack.size > 1) stack.removeAt(stack.lastIndex) else selectedTab = MainTab.SEARCH
    }
}

@Composable
fun MainTabView(profile: PublicProfile) {
    val root = LocalRootViewModel.current
    val catalog = LocalGameCatalog.current
    val navigation = viewModel { MainNavigationState() }
    LaunchedEffect(Unit) { catalog.loadIfNeeded() }

    val entryProvider = entryProvider<Any> {
        entry<MainTab>(clazzContentKey = { "tab.${it.name}" }) { tab ->
            when (tab) {
                MainTab.SEARCH -> PostListView(viewModel { PostListViewModel() }, onOpen = navigation::push)
                MainTab.POST -> CreatePostView(viewModel { CreatePostViewModel() }, onOpen = navigation::push)
                MainTab.MY_PAGE -> MyPageView(
                    viewModel { MyPageViewModel(profile, onProfileUpdated = root::didUpdateProfile) },
                    onOpen = navigation::push,
                )
            }
        }
        entry<PostDetailRoute>(clazzContentKey = { it.id }) { route ->
            PostDetailView(
                viewModel { PostDetailViewModel(route.post.id, route.post, onUpdate = route.onUpdate) },
                onBack = navigation::goBack,
            )
        }
    }
    // 選んでいないタブの画面の状態（ViewModel を含む）も保つため、すべてのタブを常に組み立てておく
    val entries = navigation.backStacks.mapValues { (_, stack) ->
        rememberDecoratedNavEntries(
            backStack = stack,
            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
            entryProvider = entryProvider,
        )
    }
    val selected = navigation.selectedTab
    // 「探す」以外のタブでは、その下に「探す」を置き、戻ると「探す」に戻れるようにする
    val visible = if (selected == MainTab.SEARCH) entries.getValue(MainTab.SEARCH) else entries.getValue(MainTab.SEARCH) + entries.getValue(selected)

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selected,
                        onClick = { navigation.selectedTab = tab },
                        icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                        label = { Text(tab.label) },
                        modifier = Modifier.testTag("tab.${tab.name}"),
                    )
                }
            }
        },
    ) { padding ->
        CompositionLocalProvider(LocalBottomInsetHandled provides true) {
            NavDisplay(
                entries = visible,
                onBack = navigation::goBack,
                modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            )
        }
    }
}
