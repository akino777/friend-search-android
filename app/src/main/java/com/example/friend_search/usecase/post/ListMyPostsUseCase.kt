package com.example.friend_search.usecase.post

import com.example.friend_search.model.PostPage
import com.example.friend_search.repository.DefaultPostRepository
import com.example.friend_search.repository.PostRepository

interface ListMyPostsUseCase {
    suspend fun myPosts(cursor: String?): PostPage
    suspend fun participations(cursor: String?): PostPage
}

class DefaultListMyPostsUseCase(
    private val repository: PostRepository = DefaultPostRepository(),
) : ListMyPostsUseCase {
    override suspend fun myPosts(cursor: String?) = repository.myPosts(cursor)

    override suspend fun participations(cursor: String?) = repository.myParticipations(cursor)
}
