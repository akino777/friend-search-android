package com.example.friend_search.viewmodel.post

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.friend_search.model.AppError
import com.example.friend_search.model.Game
import com.example.friend_search.model.NewPost
import com.example.friend_search.model.Post
import com.example.friend_search.model.VCTool
import com.example.friend_search.usecase.post.CreatePostUseCase
import com.example.friend_search.usecase.post.DefaultCreatePostUseCase
import com.example.friend_search.viewmodel.shared.PostInputRule
import com.example.friend_search.viewmodel.shared.charCount
import com.example.friend_search.viewmodel.shared.userMessage
import com.example.friend_search.viewmodel.shared.usesInviteLink
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

/** 募集作成 */
class CreatePostViewModel(
    private val createPost: CreatePostUseCase = DefaultCreatePostUseCase(),
) : ViewModel() {
    var selectedGame by mutableStateOf<Game?>(null)
    var body by mutableStateOf("")
    var vcTool by mutableStateOf(VCTool.NONE)
    var inviteUrl by mutableStateOf("")
    var capacityText by mutableStateOf(PostInputRule.DEFAULT_CAPACITY.toString())
        private set
    var isSubmitting by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)

    /** 結果が分からなかった送信。同じ内容の再送には同じ clientRequestId を使い、サーバーでの重複作成を防ぐ */
    private var pending: Pair<NewPost, UUID>? = null

    val bodyCount: Int get() = PostInputRule.normalizedBody(body).charCount()
    val isBodyCountValid: Boolean get() = bodyCount in PostInputRule.bodyRange
    val capacity: Int? get() = PostInputRule.capacity(capacityText)
    val capacityError: String? get() = if (capacity == null) PostInputRule.CAPACITY_MESSAGE else null
    val canSubmit: Boolean get() = selectedGame != null && isBodyCountValid && capacity != null && !isSubmitting

    /** 数字キーボードでも貼り付けなどで数字以外が入り得るため、半角数字だけ2桁まで残す */
    fun updateCapacityText(text: String) {
        capacityText = text.filter { it in '0'..'9' }.take(2)
    }

    suspend fun submit(): Post? {
        if (isSubmitting) return null
        errorMessage = null
        val draft = validatedDraft() ?: return null
        val requestId = requestId(draft)
        isSubmitting = true
        try {
            val post = createPost.execute(draft, requestId)
            pending = null
            reset()
            return post
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorMessage = AppError.from(e).userMessage
            return null
        } finally {
            isSubmitting = false
        }
    }

    /** 入力を検証して募集内容にする。不正なら理由を errorMessage に出して null */
    private fun validatedDraft(): NewPost? {
        val game = selectedGame ?: run {
            errorMessage = PostInputRule.GAME_MISSING_MESSAGE
            return null
        }
        if (!isBodyCountValid) {
            errorMessage = PostInputRule.BODY_MESSAGE
            return null
        }
        val capacity = capacity ?: run {
            errorMessage = PostInputRule.CAPACITY_MESSAGE
            return null
        }
        var link: String? = null
        if (vcTool.usesInviteLink) {
            link = PostInputRule.inviteLink(inviteUrl, vcTool) ?: run {
                errorMessage = PostInputRule.INVITE_LINK_MESSAGE
                return null
            }
        }
        return NewPost(game.id, PostInputRule.normalizedBody(body), vcTool, capacity, link)
    }

    private fun requestId(draft: NewPost): UUID {
        pending?.let { (pendingDraft, id) -> if (pendingDraft == draft) return id }
        val id = UUID.randomUUID()
        pending = draft to id
        return id
    }

    private fun reset() {
        body = ""
        vcTool = VCTool.NONE
        inviteUrl = ""
        capacityText = PostInputRule.DEFAULT_CAPACITY.toString()
    }
}
