package com.example.friend_search.repository

import com.example.friend_search.api.DefaultFriendSearchApi
import com.example.friend_search.api.FriendSearchApi
import com.example.friend_search.model.Game

interface GameRepository {
    suspend fun games(): List<Game>
}

class DefaultGameRepository(
    private val api: FriendSearchApi = DefaultFriendSearchApi(),
) : GameRepository {
    override suspend fun games() = withAppErrors { api.fetchGames().games.map(Game::from) }
}
