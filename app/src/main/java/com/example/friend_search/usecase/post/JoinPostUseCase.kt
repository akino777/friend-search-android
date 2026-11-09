package com.example.friend_search.usecase.post

import com.example.friend_search.model.Post
import com.example.friend_search.repository.DefaultPostRepository
import com.example.friend_search.repository.PostRepository

interface JoinPostUseCase {
    suspend fun execute(postId: String): Post
}

class DefaultJoinPostUseCase(
    private val repository: PostRepository = DefaultPostRepository(),
) : JoinPostUseCase {
    override suspend fun execute(postId: String): Post = repository.join(postId)
}
