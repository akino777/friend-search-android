package com.example.friend_search.usecase.post

import com.example.friend_search.model.Post
import com.example.friend_search.repository.DefaultPostRepository
import com.example.friend_search.repository.PostRepository

interface GetPostUseCase {
    suspend fun execute(id: String): Post
}

class DefaultGetPostUseCase(
    private val repository: PostRepository = DefaultPostRepository(),
) : GetPostUseCase {
    override suspend fun execute(id: String): Post = repository.post(id)
}
