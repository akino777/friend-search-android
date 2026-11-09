package com.example.friend_search.view.post

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.friend_search.R
import com.example.friend_search.view.game.GamePickerView
import com.example.friend_search.view.shared.FormMessage
import com.example.friend_search.view.shared.PostDetailRoute
import com.example.friend_search.view.shared.ScreenScaffold
import com.example.friend_search.view.shared.SubmitButton
import com.example.friend_search.view.shared.VCToolPicker
import com.example.friend_search.view.shared.launch
import com.example.friend_search.viewmodel.post.CreatePostViewModel
import com.example.friend_search.viewmodel.shared.PostInputRule
import com.example.friend_search.viewmodel.shared.usesInviteLink

/** 欄の見出し */
@Composable
private fun FieldHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

/** P04 募集作成。投稿できたら詳細へ進む */
@Composable
fun CreatePostView(viewModel: CreatePostViewModel, onOpen: (Any) -> Unit) {
    var showsGamePicker by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant

    ScreenScaffold(title = "募集を投稿") { modifier ->
        Column(
            modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FieldHeader("ゲーム")
            Row(
                Modifier.fillMaxWidth().clickable { showsGamePicker = true }.testTag("create.gamePicker").padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val game = viewModel.selectedGame
                Text(game?.displayName ?: "ゲームを選択", color = if (game == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.weight(1f))
                Icon(painterResource(R.drawable.chevron_right_24px), contentDescription = null, tint = secondary)
            }

            FieldHeader("募集本文")
            OutlinedTextField(
                value = viewModel.body,
                onValueChange = { viewModel.body = it },
                placeholder = { Text("例: 今夜21時から、初心者同士でまったり遊べる人を募集！ミスしても気にしません。") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp).testTag("create.body"),
            )
            Row {
                Text("${PostInputRule.bodyRange.first}〜${PostInputRule.bodyRange.last}文字", style = MaterialTheme.typography.bodySmall, color = secondary)
                Spacer(Modifier.weight(1f))
                Text(
                    "${viewModel.bodyCount} / ${PostInputRule.bodyRange.last}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (viewModel.isBodyCountValid) secondary else MaterialTheme.colorScheme.error,
                )
            }

            FieldHeader("ボイスチャット")
            VCToolPicker(label = "VCツール", selection = viewModel.vcTool) { tool -> tool?.let { viewModel.vcTool = it } }
            if (viewModel.vcTool.usesInviteLink) {
                OutlinedTextField(
                    value = viewModel.inviteUrl,
                    onValueChange = { viewModel.inviteUrl = it },
                    label = { Text("招待リンク（https://）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("リンクは参加した人に表示されます", style = MaterialTheme.typography.bodySmall, color = secondary)
            }

            FieldHeader("募集人数")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = viewModel.capacityText,
                    onValueChange = viewModel::updateCapacityText,
                    placeholder = { Text("2") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(96.dp).testTag("create.capacity"),
                )
                Text("人（あなたを除く）", color = secondary)
            }
            viewModel.capacityError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }

            viewModel.errorMessage?.let { FormMessage(it) }
            SubmitButton("投稿する", viewModel.isSubmitting, viewModel.canSubmit, Modifier.testTag("create.submit")) {
                focusManager.clearFocus()
                viewModel.launch { viewModel.submit()?.let { onOpen(PostDetailRoute(it)) } }
            }
        }
    }

    if (showsGamePicker) {
        GamePickerView(onDismiss = { showsGamePicker = false }) { game -> viewModel.selectedGame = game }
    }
}
