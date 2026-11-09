package com.example.friend_search.repository

import com.example.friend_search.api.DefaultFriendSearchApi
import com.example.friend_search.api.FriendSearchApi
import com.example.friend_search.apimodel.PostSearchRequestApiModel
import com.example.friend_search.model.NewPost
import com.example.friend_search.model.Post
import com.example.friend_search.model.PostPage
import com.example.friend_search.model.VCTool
import java.util.UUID

interface PostRepository {
    suspend fun listPosts(gameId: String?, cursor: String?): PostPage
    suspend fun search(gameId: String?, query: String, vcTool: VCTool?): List<Post>
    suspend fun post(id: String): Post
    suspend fun create(newPost: NewPost, clientRequestId: UUID): Post
    suspend fun join(postId: String): Post
    suspend fun leave(postId: String): Post
    suspend fun close(postId: String): Post
    suspend fun myPosts(cursor: String?): PostPage
    suspend fun myParticipations(cursor: String?): PostPage
}

class DefaultPostRepository(
    private val api: FriendSearchApi = DefaultFriendSearchApi(),
) : PostRepository {
    override suspend fun listPosts(gameId: String?, cursor: String?) = withAppErrors { PostPage.from(api.listPosts(gameId, cursor)) }

    override suspend fun search(gameId: String?, query: String, vcTool: VCTool?) = withAppErrors {
        val request = PostSearchRequestApiModel(gameId = gameId, query = query, vcTool = vcTool?.rawValue)
        api.searchPosts(request).items.map(Post::from)
    }

    override suspend fun post(id: String) = withAppErrors { Post.from(api.getPost(id)) }

    override suspend fun create(newPost: NewPost, clientRequestId: UUID) =
        withAppErrors { Post.from(api.createPost(newPost.toApiModel(clientRequestId))) }

    override suspend fun join(postId: String) = withAppErrors { Post.from(api.joinPost(postId)) }

    override suspend fun leave(postId: String) = withAppErrors { Post.from(api.leavePost(postId)) }

    override suspend fun close(postId: String) = withAppErrors { Post.from(api.closePost(postId)) }

    override suspend fun myPosts(cursor: String?) = withAppErrors { PostPage.from(api.myPosts(cursor)) }

    override suspend fun myParticipations(cursor: String?) = withAppErrors { PostPage.from(api.myParticipations(cursor)) }
}
