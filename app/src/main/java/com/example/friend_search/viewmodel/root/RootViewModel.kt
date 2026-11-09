package com.example.friend_search.viewmodel.root

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.friend_search.model.AppError
import com.example.friend_search.model.AvatarIdentity
import com.example.friend_search.model.ProfileLookup
import com.example.friend_search.model.PublicProfile
import com.example.friend_search.usecase.auth.DefaultRestoreSessionUseCase
import com.example.friend_search.usecase.auth.DefaultSignOutUseCase
import com.example.friend_search.usecase.auth.RestoreSessionUseCase
import com.example.friend_search.usecase.auth.SignOutUseCase
import com.example.friend_search.usecase.profile.DefaultGetProfileUseCase
import com.example.friend_search.usecase.profile.GetProfileUseCase
import com.example.friend_search.viewmodel.shared.userMessage
import kotlin.coroutines.cancellation.CancellationException

/** 起動・認証状態・プロフィール有無からルート画面を決める */
class RootViewModel(
    private val restoreSession: RestoreSessionUseCase = DefaultRestoreSessionUseCase(),
    private val getProfile: GetProfileUseCase = DefaultGetProfileUseCase(),
    private val signOutUseCase: SignOutUseCase = DefaultSignOutUseCase(),
) : ViewModel() {
    sealed interface State {
        data object Launching : State
        data class SignedOut(val message: String?) : State
        data class NeedsProfile(val avatar: AvatarIdentity?) : State
        data class ProfileLoadFailed(val error: AppError) : State
        data class Ready(val profile: PublicProfile) : State
    }

    var state by mutableStateOf<State>(State.Launching)
        private set

    private var started = false

    suspend fun start() {
        if (started) return
        started = true
        refreshSession()
    }

    fun didCreateProfile(profile: PublicProfile) {
        state = State.Ready(profile)
    }

    fun didUpdateProfile(profile: PublicProfile) {
        if (state is State.Ready) state = State.Ready(profile)
    }

    suspend fun signOut() {
        try {
            signOutUseCase.execute()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
        state = State.SignedOut(null)
    }

    private suspend fun refreshSession() {
        val signedIn = try {
            restoreSession.execute()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 通信障害ではログイン画面へ戻さず、再試行できる画面に留める
            state = if (AppError.from(e) == AppError.Network) State.ProfileLoadFailed(AppError.Network) else State.SignedOut(null)
            return
        }
        if (signedIn) loadProfile() else state = State.SignedOut(null)
    }

    /** サインイン直後・再試行時にプロフィールを読み込み、表示する画面を決める */
    suspend fun loadProfile() {
        state = try {
            when (val lookup = getProfile.execute()) {
                is ProfileLookup.Found -> State.Ready(lookup.profile)
                is ProfileLookup.NotCreated -> State.NeedsProfile(lookup.avatar)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 未作成と通信障害を区別する
            when (val appError = AppError.from(e)) {
                AppError.Unauthorized -> State.SignedOut(AppError.Unauthorized.userMessage)
                else -> State.ProfileLoadFailed(appError)
            }
        }
    }
}
