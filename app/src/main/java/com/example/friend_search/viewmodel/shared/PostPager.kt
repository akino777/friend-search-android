package com.example.friend_search.viewmodel.shared

import com.example.friend_search.model.AppError
import com.example.friend_search.model.Post
import com.example.friend_search.model.PostPage

/** カーソルページングの一覧の状態。次ページは重複を除いて追記する（値として扱い、変更は copy で行う） */
data class PostPager(
    val posts: List<Post> = emptyList(),
    val nextCursor: String? = null,
    val isLoadingMore: Boolean = false,
    val loadMoreError: AppError? = null,
) {
    fun reset(page: PostPage) = copy(posts = page.items, nextCursor = page.nextCursor, loadMoreError = null)

    fun append(page: PostPage): PostPager {
        val known = posts.map(Post::id).toSet()
        return copy(posts = posts + page.items.filter { it.id !in known }, nextCursor = page.nextCursor)
    }

    companion object {
        const val MAX_EMPTY_PAGES = 5

        /** 1ページ取得する。空ページでも続きがある場合は、画面の末尾表示を待たずに次のページまで進む */
        suspend fun fetch(cursor: String?, request: suspend (String?) -> PostPage): PostPage {
            var page = request(cursor)
            repeat(MAX_EMPTY_PAGES) {
                val next = page.nextCursor
                if (page.items.isNotEmpty() || next == null) return page
                page = request(next)
            }
            return page
        }
    }
}
