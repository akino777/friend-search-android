package com.example.friend_search.usecase.post

import com.example.friend_search.model.PostPage
import com.example.friend_search.repository.DefaultPostRepository
import com.example.friend_search.repository.PostRepository

interface ListPostsUseCase {
    suspend fun execute(gameId: String?, cursor: String?): PostPage
}

class DefaultListPostsUseCase(
    private val repository: PostRepository = DefaultPostRepository(),
) : ListPostsUseCase {
    override suspend fun execute(gameId: String?, cursor: String?): PostPage = repository.listPosts(gameId, cursor)
}
