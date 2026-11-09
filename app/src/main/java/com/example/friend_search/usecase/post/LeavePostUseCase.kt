package com.example.friend_search.usecase.post

import com.example.friend_search.model.Post
import com.example.friend_search.repository.DefaultPostRepository
import com.example.friend_search.repository.PostRepository

interface LeavePostUseCase {
    suspend fun execute(postId: String): Post
}

class DefaultLeavePostUseCase(
    private val repository: PostRepository = DefaultPostRepository(),
) : LeavePostUseCase {
    override suspend fun execute(postId: String): Post = repository.leave(postId)
}
