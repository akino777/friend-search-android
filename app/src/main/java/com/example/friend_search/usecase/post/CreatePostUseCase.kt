package com.example.friend_search.usecase.post

import com.example.friend_search.model.NewPost
import com.example.friend_search.model.Post
import java.util.UUID
import com.example.friend_search.repository.DefaultPostRepository
import com.example.friend_search.repository.PostRepository

interface CreatePostUseCase {
    /** 同じ clientRequestId の再送はサーバーが重複作成しない */
    suspend fun execute(newPost: NewPost, clientRequestId: UUID): Post
}

class DefaultCreatePostUseCase(
    private val repository: PostRepository = DefaultPostRepository(),
) : CreatePostUseCase {
    override suspend fun execute(newPost: NewPost, clientRequestId: UUID): Post = repository.create(newPost, clientRequestId)
}
