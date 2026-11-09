package com.example.friend_search.view.post

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.friend_search.R
import com.example.friend_search.model.Post
import com.example.friend_search.view.profile.IdenticonView
import com.example.friend_search.view.root.LocalGameCatalog
import com.example.friend_search.view.shared.FormMessage
import com.example.friend_search.view.shared.IconLabel
import com.example.friend_search.view.shared.JoinedGreen
import com.example.friend_search.view.shared.ScreenScaffold
import com.example.friend_search.view.shared.StatusBadge
import com.example.friend_search.view.shared.icon
import com.example.friend_search.view.shared.launch
import com.example.friend_search.viewmodel.post.PostDetailViewModel
import com.example.friend_search.viewmodel.shared.canClose
import com.example.friend_search.viewmodel.shared.canJoin
import com.example.friend_search.viewmodel.shared.displayName
import com.example.friend_search.viewmodel.shared.displayState
import com.example.friend_search.viewmodel.shared.joinButtonTitle
import com.example.friend_search.viewmodel.shared.showsInviteLinkHint
import com.example.friend_search.viewmodel.shared.userMessage
import com.example.friend_search.viewmodel.shared.visibleInviteLink
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

/** P03 募集詳細（参加・取消・投稿者による終了） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailView(viewModel: PostDetailViewModel, onBack: () -> Unit) {
    var confirmsClose by rememberSaveable { mutableStateOf(false) }
    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(viewModel.shouldDismiss) { if (viewModel.shouldDismiss) onBack() }

    ScreenScaffold(title = "募集詳細", onBack = onBack) { modifier ->
        val post = viewModel.post
        val error = viewModel.loadError
        when {
            post != null -> PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        try {
                            viewModel.load()
                        } finally {
                            isRefreshing = false
                        }
                    }
                },
                modifier = modifier.fillMaxSize(),
            ) {
                Content(viewModel, post) { confirmsClose = true }
            }
            error != null -> Column(
                modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(painterResource(R.drawable.wifi_off_24px), contentDescription = null, modifier = Modifier.size(48.dp))
                Text("読み込めませんでした", style = MaterialTheme.typography.titleMedium)
                Text(error.userMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { viewModel.launch { viewModel.load() } }) { Text("再試行") }
            }
            else -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
    }

    if (confirmsClose) {
        AlertDialog(
            onDismissRequest = { confirmsClose = false },
            title = { Text("この募集を終了しますか？") },
            text = { Text("終了すると新しい参加を受け付けません") },
            confirmButton = {
                TextButton(onClick = {
                    confirmsClose = false
                    viewModel.launch { viewModel.close() }
                }) { Text("募集を終了する", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmsClose = false }) { Text("キャンセル") } },
        )
    }
}

@Composable
private fun Content(viewModel: PostDetailViewModel, post: Post, onRequestClose: () -> Unit) {
    val catalog = LocalGameCatalog.current
    val uriHandler = LocalUriHandler.current
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IdenticonView(post.author.publicUserId, post.author.avatarVersion, size = 56.dp)
            Column {
                Text(post.author.displayName, style = MaterialTheme.typography.titleMedium)
                Text(formatDateTime(post), style = MaterialTheme.typography.labelSmall, color = secondary)
            }
            Spacer(Modifier.weight(1f))
            StatusBadge(post.displayState)
        }

        IconLabel(R.drawable.sports_esports_24px, catalog.displayName(post.gameId))

        SelectionContainer {
            Text(
                post.body,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            IconLabel(post.vcTool.icon, post.vcTool.displayName)
            IconLabel(R.drawable.group_24px, "${post.participantCount} / ${post.capacity}人", modifier = Modifier.testTag("detail.count"))
        }
        Text("募集人数は投稿者を除いた人数です", style = MaterialTheme.typography.bodySmall, color = secondary)

        val link = post.visibleInviteLink
        if (link != null) {
            Button(onClick = { uriHandler.openUri(link) }) { Text("${post.vcTool.displayName} の招待リンクを開く") }
        } else if (post.showsInviteLinkHint) {
            Text("参加後に招待リンクを確認できます（リンクが登録されている場合）", style = MaterialTheme.typography.bodySmall, color = secondary)
        }

        viewModel.actionMessage?.let { FormMessage(it) }

        Actions(viewModel, post, onRequestClose)
    }
}

@Composable
private fun Actions(viewModel: PostDetailViewModel, post: Post, onRequestClose: () -> Unit) {
    val enabled = !viewModel.isProcessing
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            post.isOwnedByMe -> if (post.canClose) {
                OutlinedButton(
                    onClick = onRequestClose,
                    enabled = enabled,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().testTag("detail.close"),
                ) { Text("募集を終了") }
            } else {
                Text("この募集は終了しました", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            post.isJoinedByMe -> {
                IconLabel(R.drawable.check_circle_fill1_24px, "参加中です", color = JoinedGreen)
                OutlinedButton(
                    onClick = { viewModel.launch { viewModel.leave() } },
                    enabled = enabled,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().testTag("detail.leave"),
                ) { ActionLabel(viewModel, "参加を取り消す") }
            }
            else -> Button(
                onClick = { viewModel.launch { viewModel.join() } },
                enabled = enabled && post.canJoin,
                modifier = Modifier.fillMaxWidth().testTag("detail.join"),
            ) { ActionLabel(viewModel, post.joinButtonTitle) }
        }
    }
}

@Composable
private fun ActionLabel(viewModel: PostDetailViewModel, title: String) {
    if (viewModel.isProcessing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(title, fontWeight = FontWeight.Bold)
}

/** 月日と時刻（端末の言語に合わせた並び） */
private fun formatDateTime(post: Post): String {
    val locale = Locale.getDefault()
    val formatter = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMdHHmm"), locale)
    return post.createdAt.atZone(ZoneId.systemDefault()).format(formatter)
}
