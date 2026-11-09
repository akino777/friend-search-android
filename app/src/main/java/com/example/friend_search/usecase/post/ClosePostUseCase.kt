package com.example.friend_search.usecase.post

import com.example.friend_search.model.Post
import com.example.friend_search.repository.DefaultPostRepository
import com.example.friend_search.repository.PostRepository

interface ClosePostUseCase {
    suspend fun execute(postId: String): Post
}

class DefaultClosePostUseCase(
    private val repository: PostRepository = DefaultPostRepository(),
) : ClosePostUseCase {
    override suspend fun execute(postId: String): Post = repository.close(postId)
}
