package com.example.friend_search.view.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.unit.dp
import com.example.friend_search.view.root.LocalRootViewModel
import com.example.friend_search.view.shared.FootnoteText
import com.example.friend_search.view.shared.FormMessage
import com.example.friend_search.view.shared.LoadingRow
import com.example.friend_search.view.shared.PostDetailRoute
import com.example.friend_search.view.shared.PostRowView
import com.example.friend_search.view.shared.ScreenScaffold
import com.example.friend_search.view.shared.launch
import com.example.friend_search.viewmodel.profile.MyPageViewModel
import com.example.friend_search.viewmodel.shared.ListPhase
import com.example.friend_search.viewmodel.shared.userMessage
import kotlinx.coroutines.launch

/** M01 マイページ（表示名・Identicon、自分の募集、参加中一覧、ログアウト） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPageView(viewModel: MyPageViewModel, onOpen: (Any) -> Unit) {
    val root = LocalRootViewModel.current
    var editsName by rememberSaveable { mutableStateOf(false) }
    var confirmsSignOut by rememberSaveable { mutableStateOf(false) }
    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { viewModel.reload() }

    ScreenScaffold(title = "マイページ") { modifier ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    try {
                        viewModel.reload()
                    } finally {
                        isRefreshing = false
                    }
                }
            },
            modifier = modifier.fillMaxSize(),
        ) {
            LazyColumn(Modifier.fillMaxSize().testTag("mypage.content")) {
                item {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        IdenticonView(viewModel.profile.publicUserId, viewModel.profile.avatarVersion, size = 64.dp)
                        Column {
                            Text(viewModel.profile.displayName, style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("mypage.displayName"))
                            TextButton(onClick = {
                                viewModel.beginEditingName()
                                editsName = true
                            }) { Text("表示名を変更") }
                        }
                    }
                }
                item {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        MyPageViewModel.Segment.entries.forEachIndexed { index, segment ->
                            SegmentedButton(
                                selected = viewModel.segment == segment,
                                onClick = {
                                    viewModel.segment = segment
                                    viewModel.segmentChanged()
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, MyPageViewModel.Segment.entries.size),
                            ) { Text(segment.label) }
                        }
                    }
                }
                when (val phase = viewModel.phase) {
                    ListPhase.Idle, ListPhase.Loading -> item { LoadingRow() }
                    ListPhase.Empty -> item { FootnoteText(viewModel.emptyMessage) }
                    is ListPhase.Failed -> item {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            FormMessage(phase.error.userMessage)
                            TextButton(onClick = { viewModel.launch { viewModel.reload() } }) { Text("再読み込み") }
                        }
                    }
                    ListPhase.Loaded -> {
                        items(viewModel.visiblePosts, key = { "${viewModel.segment}.${it.id}" }) { post ->
                            PostRowView(post, showsGame = true) { onOpen(PostDetailRoute(post)) }
                            LaunchedEffect(post.id, viewModel.segment) { viewModel.loadMoreIfNeeded(post) }
                        }
                        val loadMoreError = viewModel.loadMoreError
                        if (viewModel.isLoadingMore) {
                            item { LoadingRow() }
                        } else if (loadMoreError != null) {
                            item {
                                Column(Modifier.padding(horizontal = 16.dp)) {
                                    FormMessage(loadMoreError.userMessage)
                                    TextButton(onClick = { viewModel.launch { viewModel.loadMore() } }) { Text("続きを再読み込み") }
                                }
                            }
                        }
                    }
                }
                item {
                    TextButton(
                        onClick = { confirmsSignOut = true },
                        modifier = Modifier.padding(8.dp).testTag("mypage.signOut"),
                    ) { Text("ログアウト", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }

    if (confirmsSignOut) {
        AlertDialog(
            onDismissRequest = { confirmsSignOut = false },
            title = { Text("ログアウトしますか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmsSignOut = false
                        root.launch { root.signOut() }
                    },
                    modifier = Modifier.testTag("mypage.signOut.confirm"),
                ) { Text("ログアウト", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmsSignOut = false }) { Text("キャンセル") } },
        )
    }

    if (editsName) {
        AlertDialog(
            onDismissRequest = { editsName = false },
            title = { Text("表示名を変更") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = viewModel.editingName, onValueChange = { viewModel.editingName = it }, label = { Text("表示名") }, singleLine = true)
                    viewModel.nameError?.let { FormMessage(it) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.launch { if (viewModel.saveName()) editsName = false } },
                    enabled = viewModel.canSaveName,
                ) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editsName = false }) { Text("キャンセル") } },
        )
    }
}
