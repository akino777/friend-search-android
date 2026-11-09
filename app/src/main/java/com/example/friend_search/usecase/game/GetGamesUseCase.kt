package com.example.friend_search.usecase.game

import com.example.friend_search.model.Game
import com.example.friend_search.repository.DefaultGameRepository
import com.example.friend_search.repository.GameRepository

interface GetGamesUseCase {
    suspend fun execute(): List<Game>
}

class DefaultGetGamesUseCase(
    private val repository: GameRepository = DefaultGameRepository(),
) : GetGamesUseCase {
    override suspend fun execute() = repository.games()
}
