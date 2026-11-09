package com.example.friend_search.usecase.post

import com.example.friend_search.model.Post
import com.example.friend_search.model.VCTool
import com.example.friend_search.repository.DefaultPostRepository
import com.example.friend_search.repository.PostRepository

interface SearchPostsUseCase {
    /** gameId が null なら全ゲームを検索する */
    suspend fun execute(gameId: String?, query: String, vcTool: VCTool?): List<Post>
}

class DefaultSearchPostsUseCase(
    private val repository: PostRepository = DefaultPostRepository(),
) : SearchPostsUseCase {
    override suspend fun execute(gameId: String?, query: String, vcTool: VCTool?): List<Post> = repository.search(gameId, query, vcTool)
}
