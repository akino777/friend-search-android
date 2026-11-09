package com.example.friend_search.view.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.friend_search.model.Game
import com.example.friend_search.view.root.LocalGameCatalog
import com.example.friend_search.view.shared.FormMessage
import com.example.friend_search.view.shared.LoadingRow
import com.example.friend_search.view.shared.launch
import com.example.friend_search.viewmodel.game.GamePickerViewModel
import com.example.friend_search.viewmodel.shared.userMessage

/** G01 ゲーム選択（端末内のマスタを表示名・別名で候補検索。AIで判定しない）。選んだら閉じる */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamePickerView(onDismiss: () -> Unit, onSelect: (Game) -> Unit) {
    val catalog = LocalGameCatalog.current
    val viewModel = remember(catalog) { GamePickerViewModel(catalog) }
    LaunchedEffect(Unit) { viewModel.load() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Text("ゲームを選択", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
        OutlinedTextField(
            value = viewModel.query,
            onValueChange = { viewModel.query = it },
            placeholder = { Text("ゲーム名・略称で検索") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
            modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("game.search"),
        )
        LazyColumn {
            if (viewModel.store.games == null) {
                item {
                    val error = viewModel.store.error
                    if (viewModel.store.isLoading) {
                        LoadingRow()
                    } else if (error != null) {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            FormMessage(error.userMessage)
                            TextButton(onClick = { catalog.launch { catalog.refresh() } }) { Text("再読み込み") }
                        }
                    }
                }
            }
            items(viewModel.candidates, key = Game::id) { game ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelect(game)
                            onDismiss()
                        }
                        .testTag("game.${game.id}")
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(game.displayName)
                    if (game.aliases.isNotEmpty()) {
                        Text(
                            game.aliases.joinToString(" / "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (viewModel.showsNoMatch) {
                item {
                    Text(
                        "「${viewModel.query}」は見つかりませんでした",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}
