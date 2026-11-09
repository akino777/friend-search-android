package com.example.friend_search.view.shared

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.friend_search.R
import kotlinx.coroutines.launch

/** 画面を離れても取り消さない操作を ViewModel の寿命の間だけ続ける（ボタン操作など） */
fun ViewModel.launch(block: suspend () -> Unit) {
    viewModelScope.launch { block() }
}

/** タブ画面の中では、下端の余白をタブバー側で取るため画面側では取らない */
val LocalBottomInsetHandled = compositionLocalOf { false }

/** 画面の枠（上部バーと戻るボタン） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("nav.back")) {
                            Icon(painterResource(R.drawable.arrow_back_24px), contentDescription = "戻る")
                        }
                    }
                },
                actions = { actions() },
            )
        },
        contentWindowInsets = if (LocalBottomInsetHandled.current) WindowInsets(0) else ScaffoldDefaults.contentWindowInsets,
    ) { padding ->
        content(Modifier.padding(padding))
    }
}

/** フォームのエラー・案内表示 */
@Composable
fun FormMessage(text: String, isError: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        Icon(painterResource(if (isError) R.drawable.error_24px else R.drawable.info_24px), contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            modifier = Modifier.testTag(if (isError) "form.error" else "form.info"),
        )
    }
}

/** リストの中央に出す読み込み中表示 */
@Composable
fun LoadingRow() {
    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** 処理中はスピナーを出して二重タップを防ぐボタン */
@Composable
fun SubmitButton(title: String, isLoading: Boolean, isEnabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = isEnabled && !isLoading, modifier = modifier.fillMaxWidth()) {
        if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(title, fontWeight = FontWeight.Bold)
    }
}

/** 一覧の見出し */
fun LazyListScope.sectionHeader(title: String) {
    item(contentType = "header") {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
        )
    }
}

/** 一覧の補足（見出しの下・欄の後の小さい文字） */
@Composable
fun FootnoteText(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = color, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
}

/** アイコンと文字（iOS の Label に相当） */
@Composable
fun IconLabel(@DrawableRes icon: Int, text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    // アイコンと文字を1つの要素として読み上げ・テストで扱う
    Row(modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(16.dp), tint = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else color)
        Text(text, color = color)
    }
}
