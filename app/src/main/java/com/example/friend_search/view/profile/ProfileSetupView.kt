package com.example.friend_search.view.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.friend_search.R
import com.example.friend_search.model.AvatarIdentity
import com.example.friend_search.model.PublicProfile
import com.example.friend_search.view.shared.FormMessage
import com.example.friend_search.view.shared.ScreenScaffold
import com.example.friend_search.view.shared.SubmitButton
import com.example.friend_search.view.shared.launch
import com.example.friend_search.viewmodel.profile.ProfileSetupViewModel
import com.example.friend_search.viewmodel.shared.DisplayNameRule

/** A04 表示名設定（初回プロフィール作成） */
@Composable
fun ProfileSetupView(viewModel: ProfileSetupViewModel, avatar: AvatarIdentity?, onCreated: (PublicProfile) -> Unit) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    ScreenScaffold(title = "プロフィール設定") { modifier ->
        Column(
            modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (avatar != null) {
                IdenticonView(avatar.publicUserId, avatar.avatarVersion, size = 80.dp)
            } else {
                Box(
                    Modifier.size(80.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.grid_view_24px), contentDescription = null, tint = secondary, modifier = Modifier.size(40.dp))
                }
            }
            Text("あなたのアイコンです。自動で作られ、画像のアップロードはありません", style = MaterialTheme.typography.bodySmall, color = secondary, textAlign = TextAlign.Center)
            OutlinedTextField(
                value = viewModel.displayName,
                onValueChange = { viewModel.displayName = it },
                label = { Text("表示名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("profile.displayName"),
            )
            Text(
                "${DisplayNameRule.range.first}〜${DisplayNameRule.range.last}文字。他のユーザーに表示されます",
                style = MaterialTheme.typography.bodySmall,
                color = secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            viewModel.errorMessage?.let { FormMessage(it) }
            SubmitButton("はじめる", viewModel.isSubmitting, viewModel.canSubmit, Modifier.testTag("profile.submit")) {
                viewModel.launch { viewModel.submit()?.let(onCreated) }
            }
        }
    }
}
