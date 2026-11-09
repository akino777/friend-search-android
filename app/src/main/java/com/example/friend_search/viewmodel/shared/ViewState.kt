package com.example.friend_search.viewmodel.shared

import com.example.friend_search.model.AppError

/** 画面の読み込み状態（idle / loading / loaded / empty / error を明示する） */
sealed interface ListPhase {
    data object Idle : ListPhase
    data object Loading : ListPhase
    data object Loaded : ListPhase
    data object Empty : ListPhase
    data class Failed(val error: AppError) : ListPhase
}
