package com.example.friend_search.view.discovery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.friend_search.R
import com.example.friend_search.model.VCTool
import com.example.friend_search.view.game.GamePickerView
import com.example.friend_search.view.root.LocalGameCatalog
import com.example.friend_search.view.shared.FootnoteText
import com.example.friend_search.view.shared.FormMessage
import com.example.friend_search.view.shared.LoadingRow
import com.example.friend_search.view.shared.PostDetailRoute
import com.example.friend_search.view.shared.PostRowView
import com.example.friend_search.view.shared.ScreenScaffold
import com.example.friend_search.view.shared.VCToolPicker
import com.example.friend_search.view.shared.launch
import com.example.friend_search.view.shared.sectionHeader
import com.example.friend_search.viewmodel.post.PostListViewModel
import com.example.friend_search.viewmodel.shared.ListPhase
import com.example.friend_search.viewmodel.shared.PostInputRule
import com.example.friend_search.viewmodel.shared.userMessage
import kotlinx.coroutines.launch

/** P01 募集一覧（全ゲームの新着一覧・ゲームでの絞り込み・AI検索。検索結果は新着の欄に置き換えて出す） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostListView(viewModel: PostListViewModel, onOpen: (Any) -> Unit) {
    val catalog = LocalGameCatalog.current
    var showsGamePicker by rememberSaveable { mutableStateOf(false) }
    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { viewModel.reload() }

    ScreenScaffold(title = "募集を探す") { modifier ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    try {
                        viewModel.refresh()
                    } finally {
                        isRefreshing = false
                    }
                }
            },
            modifier = modifier.fillMaxSize(),
        ) {
            LazyColumn(Modifier.fillMaxSize().imePadding().testTag("list.content")) {
                sectionHeader("ゲームで絞り込む")
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { showsGamePicker = true }
                            .testTag("list.gamePicker")
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(painterResource(R.drawable.sports_esports_24px), contentDescription = null)
                        Text(viewModel.selectedGameId?.let(catalog::displayName) ?: "すべてのゲーム")
                        Spacer(Modifier.weight(1f))
                        Icon(painterResource(R.drawable.unfold_more_24px), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (viewModel.selectedGameId != null) {
                    item {
                        TextButton(
                            onClick = { viewModel.launch { viewModel.select(null) } },
                            modifier = Modifier.padding(horizontal = 8.dp).testTag("list.clearGame"),
                        ) {
                            Icon(painterResource(R.drawable.cancel_24px), contentDescription = null)
                            Text("絞り込みを解除", modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }

                // ゲーム未選択なら全ゲームを検索する
                sectionHeader("どんな人と遊びたい？")
                item { PostSearchEntry(viewModel) }

                if (viewModel.isSearchActive) {
                    sectionHeader("AI検索の結果")
                    searchResults(viewModel, onOpen)
                    item {
                        FootnoteText("文章の意味が近い募集を関連度順に表示します。投稿直後の募集は検索に表示されるまで少し時間がかかることがあります。")
                    }
                } else {
                    sectionHeader("新着の募集")
                    latestPosts(viewModel, onOpen)
                }
            }
        }
    }

    if (showsGamePicker) {
        GamePickerView(onDismiss = { showsGamePicker = false }) { game ->
            viewModel.launch { viewModel.select(game.id) }
        }
    }
}

private fun LazyListScope.latestPosts(viewModel: PostListViewModel, onOpen: (Any) -> Unit) {
    when (val phase = viewModel.phase) {
        ListPhase.Idle, ListPhase.Loading -> item { LoadingRow() }
        ListPhase.Empty -> item { FootnoteText("まだ募集がありません。最初の募集を投稿してみましょう") }
        is ListPhase.Failed -> item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                FormMessage(phase.error.userMessage)
                TextButton(onClick = { viewModel.launch { viewModel.reload() } }) { Text("再読み込み") }
            }
        }
        ListPhase.Loaded -> {
            items(viewModel.posts, key = { "post.${it.id}" }) { post ->
                PostRowView(post) { onOpen(PostDetailRoute(post)) }
                LaunchedEffect(post.id) { viewModel.loadMoreIfNeeded(post) }
            }
            if (viewModel.isLoadingMore) item { LoadingRow() }
        }
    }
}

private fun LazyListScope.searchResults(viewModel: PostListViewModel, onOpen: (Any) -> Unit) {
    when (viewModel.searchPhase) {
        PostListViewModel.SearchPhase.Idle -> Unit
        PostListViewModel.SearchPhase.Searching -> item { LoadingRow() }
        PostListViewModel.SearchPhase.Empty -> item { FootnoteText("条件に合う募集が見つかりませんでした") }
        PostListViewModel.SearchPhase.Unavailable, is PostListViewModel.SearchPhase.Failed -> item {
            viewModel.searchErrorMessage?.let { Box(Modifier.padding(horizontal = 16.dp)) { FormMessage(it) } }
        }
        PostListViewModel.SearchPhase.Results -> items(viewModel.searchResults, key = { "search.${it.id}" }) { post ->
            // 詳細での参加・取消を検索結果へ反映する（検索は費用がかかるので取り直さない）
            PostRowView(post) { onOpen(PostDetailRoute(post, onUpdate = viewModel::applyToSearchResults)) }
        }
    }
}

/** AI 検索の入力欄。入力状態をここに閉じ込め、1文字ごとに一覧全体を再評価しない */
@Composable
private fun PostSearchEntry(viewModel: PostListViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var vcFilter by rememberSaveable { mutableStateOf<VCTool?>(null) }
    Column {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("例: 初心者に優しい人とまったり遊びたい") },
            minLines = 1,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("list.searchField"),
        )
        VCToolPicker(label = "VC", selection = vcFilter, unspecifiedLabel = "指定なし") { vcFilter = it }
        OutlinedButton(
            onClick = { viewModel.launch { viewModel.search(query, vcFilter) } },
            enabled = PostInputRule.normalizedQuery(query) != null && viewModel.searchPhase != PostListViewModel.SearchPhase.Searching,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("list.searchButton"),
        ) {
            Icon(painterResource(R.drawable.auto_awesome_24px), contentDescription = null)
            Text("AIで募集を探す", modifier = Modifier.padding(start = 6.dp))
        }
        if (viewModel.isSearchActive) {
            TextButton(
                onClick = { viewModel.clearSearch() },
                modifier = Modifier.padding(horizontal = 8.dp).testTag("list.clearSearch"),
            ) {
                Icon(painterResource(R.drawable.cancel_24px), contentDescription = null)
                Text("検索を解除", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}
