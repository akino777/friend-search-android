package com.example.friend_search.view.shared

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.friend_search.R
import com.example.friend_search.model.Post
import com.example.friend_search.model.VCTool
import com.example.friend_search.view.profile.IdenticonView
import com.example.friend_search.view.root.LocalGameCatalog
import com.example.friend_search.viewmodel.shared.PostDisplayState
import com.example.friend_search.viewmodel.shared.displayName
import com.example.friend_search.viewmodel.shared.displayState

/** 募集の1行。タップで詳細へ進む */
@Composable
fun PostRowView(post: Post, showsGame: Boolean = false, onClick: () -> Unit) {
    val catalog = LocalGameCatalog.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("post.row.${post.id}")
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IdenticonView(post.author.publicUserId, post.author.avatarVersion, size = 40.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(post.author.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.weight(1f))
                StatusBadge(post.displayState)
            }
            if (showsGame) {
                Text(catalog.displayName(post.gameId), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(post.body, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            ProvideTextStyle(MaterialTheme.typography.labelSmall) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
                    IconLabel(post.vcTool.icon, post.vcTool.displayName, color = secondary)
                    IconLabel(R.drawable.group_24px, "${post.participantCount}/${post.capacity}人", color = secondary)
                    if (post.isJoinedByMe) IconLabel(R.drawable.check_circle_fill1_24px, "参加中", color = JoinedGreen)
                    Spacer(Modifier.weight(1f))
                    Text(
                        DateUtils.getRelativeTimeSpanString(post.createdAt.toEpochMilli(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
                        color = secondary,
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(state: PostDisplayState) {
    val color = when (state) {
        PostDisplayState.RECRUITING -> JoinedGreen
        PostDisplayState.FULL -> Color(0xFFEF6C00)
        PostDisplayState.CLOSED -> Color.Gray
    }
    Text(
        state.label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** VC ツールのアイコン */
val VCTool.icon: Int get() = if (this == VCTool.NONE) R.drawable.mic_off_24px else R.drawable.mic_24px

/** 募集中・参加中の色 */
val JoinedGreen = Color(0xFF2E7D32)
