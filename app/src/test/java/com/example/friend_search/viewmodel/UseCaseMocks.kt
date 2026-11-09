package com.example.friend_search.viewmodel

import com.example.friend_search.Unimplemented
import com.example.friend_search.model.Game
import com.example.friend_search.model.NewPost
import com.example.friend_search.model.Post
import com.example.friend_search.model.PostPage
import com.example.friend_search.model.ProfileLookup
import com.example.friend_search.model.PublicProfile
import com.example.friend_search.model.SignInOutcome
import com.example.friend_search.model.VCTool
import com.example.friend_search.usecase.auth.RestoreSessionUseCase
import com.example.friend_search.usecase.auth.SignInUseCase
import com.example.friend_search.usecase.auth.SignOutUseCase
import com.example.friend_search.usecase.game.GetGamesUseCase
import com.example.friend_search.usecase.post.ClosePostUseCase
import com.example.friend_search.usecase.post.CreatePostUseCase
import com.example.friend_search.usecase.post.GetPostUseCase
import com.example.friend_search.usecase.post.JoinPostUseCase
import com.example.friend_search.usecase.post.LeavePostUseCase
import com.example.friend_search.usecase.post.ListMyPostsUseCase
import com.example.friend_search.usecase.post.ListPostsUseCase
import com.example.friend_search.usecase.post.SearchPostsUseCase
import com.example.friend_search.usecase.profile.BootstrapProfileUseCase
import com.example.friend_search.usecase.profile.GetProfileUseCase
import com.example.friend_search.usecase.profile.UpdateProfileUseCase
import java.util.UUID

// ViewModel 層のテストで差し込む UseCase のモック。未設定の処理は Unimplemented を投げる。

class MockCreatePostUseCase : CreatePostUseCase {
    var handler: suspend (NewPost, UUID) -> Post = { _, _ -> throw Unimplemented() }
    val received = mutableListOf<Pair<NewPost, UUID>>()

    override suspend fun execute(newPost: NewPost, clientRequestId: UUID): Post {
        received += newPost to clientRequestId
        return handler(newPost, clientRequestId)
    }
}

class MockSearchPostsUseCase : SearchPostsUseCase {
    data class Request(val gameId: String?, val query: String, val vcTool: VCTool?)

    var handler: suspend (String) -> List<Post> = { throw Unimplemented() }
    val requests = mutableListOf<Request>()
    val queries: List<String> get() = requests.map(Request::query)
    val calls: Int get() = requests.size

    override suspend fun execute(gameId: String?, query: String, vcTool: VCTool?): List<Post> {
        requests += Request(gameId, query, vcTool)
        return handler(query)
    }
}

class MockGetPostUseCase : GetPostUseCase {
    var handler: suspend (String) -> Post = { throw Unimplemented() }
    var calls = 0
        private set

    override suspend fun execute(id: String): Post {
        calls += 1
        return handler(id)
    }
}

/** 参加・取消・終了（いずれも募集 ID を受け取り更新後の募集を返す） */
class MockPostActionUseCase : JoinPostUseCase, LeavePostUseCase, ClosePostUseCase {
    var handler: suspend (String) -> Post = { throw Unimplemented() }

    override suspend fun execute(postId: String): Post = handler(postId)
}

class MockListPostsUseCase : ListPostsUseCase {
    var handler: suspend (String?) -> PostPage = { throw Unimplemented() }
    val requestedGameIds = mutableListOf<String?>()

    override suspend fun execute(gameId: String?, cursor: String?): PostPage {
        requestedGameIds += gameId
        return handler(cursor)
    }
}

class MockListMyPostsUseCase : ListMyPostsUseCase {
    var myPostsHandler: suspend (String?) -> PostPage = { PostPage(emptyList(), null) }
    var participationsHandler: suspend (String?) -> PostPage = { PostPage(emptyList(), null) }

    override suspend fun myPosts(cursor: String?) = myPostsHandler(cursor)
    override suspend fun participations(cursor: String?) = participationsHandler(cursor)
}

class MockUpdateProfileUseCase : UpdateProfileUseCase {
    var handler: suspend (String) -> PublicProfile = { throw Unimplemented() }
    val names = mutableListOf<String>()

    override suspend fun execute(displayName: String): PublicProfile {
        names += displayName
        return handler(displayName)
    }
}

class MockRestoreSessionUseCase : RestoreSessionUseCase {
    var handler: suspend () -> Boolean = { throw Unimplemented() }

    override suspend fun execute() = handler()
}

class MockGetProfileUseCase : GetProfileUseCase {
    var handler: suspend () -> ProfileLookup = { throw Unimplemented() }

    override suspend fun execute() = handler()
}

class MockSignOutUseCase : SignOutUseCase {
    var calls = 0
        private set

    override suspend fun execute() {
        calls += 1
    }
}

class MockGetGamesUseCase : GetGamesUseCase {
    var executeHandler: suspend () -> List<Game> = { throw Unimplemented() }

    override suspend fun execute() = executeHandler()
}

class MockSignInUseCase : SignInUseCase {
    var handler: suspend (String, String) -> SignInOutcome = { _, _ -> throw Unimplemented() }
    val emails = mutableListOf<String>()

    override suspend fun execute(email: String, password: String): SignInOutcome {
        emails += email
        return handler(email, password)
    }
}

class MockBootstrapProfileUseCase : BootstrapProfileUseCase {
    var handler: suspend (String) -> PublicProfile = { throw Unimplemented() }
    val names = mutableListOf<String>()

    override suspend fun execute(displayName: String): PublicProfile {
        names += displayName
        return handler(displayName)
    }
}
