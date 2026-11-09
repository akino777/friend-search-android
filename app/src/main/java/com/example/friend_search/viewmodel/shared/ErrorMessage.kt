package com.example.friend_search.viewmodel.shared

import com.example.friend_search.model.AppError
import com.example.friend_search.model.AuthFailure

// 通信・認証エラーを画面に表示する文言にする（Throwable.message と区別するため userMessage と呼ぶ）

val AppError.userMessage: String
    get() = when (this) {
        is AppError.Validation -> serverMessage ?: "入力内容を確認してください"
        AppError.UnknownGame -> "このゲームは選択できません。ゲーム一覧を更新してください"
        AppError.Unauthorized -> "ログインの有効期限が切れました。再度ログインしてください"
        is AppError.Forbidden -> serverMessage ?: "この操作は許可されていません"
        AppError.ProfileRequired -> "先にプロフィールを作成してください"
        AppError.ProfileNotFound -> "プロフィールが未作成です"
        AppError.NotFound -> "募集が見つかりませんでした"
        AppError.PostFull -> "募集人数に達しました"
        AppError.PostClosed -> "この募集は終了しています"
        AppError.IdempotencyConflict -> "送信内容が変わったため投稿できませんでした。もう一度投稿してください"
        AppError.ConcurrentUpdate -> "他の操作と競合しました。最新の状態を確認してください"
        AppError.RateLimited -> "操作が多すぎます。少し時間を空けてからお試しください"
        AppError.SearchUnavailable -> "現在AI検索を利用できません"
        AppError.TemporarilyUnavailable -> "一時的に処理できませんでした。再度お試しください"
        is AppError.Server -> serverMessage ?: "サーバーエラーが発生しました（$status）"
        AppError.Network -> "通信できませんでした。ネットワーク接続を確認してください"
        AppError.InvalidResponse -> "サーバーの応答を読み取れませんでした"
        AppError.Unexpected -> "予期しないエラーが発生しました"
        is AppError.Auth -> failure.userMessage
    }

val AuthFailure.userMessage: String
    get() = when (this) {
        AuthFailure.InvalidCredentials -> "メールアドレスまたはパスワードが正しくありません"
        AuthFailure.LimitExceeded -> "試行回数が上限に達しました。しばらく時間を空けてください"
        AuthFailure.UserNotFound -> "アカウントが見つかりません"
        is AuthFailure.Unexpected -> "認証でエラーが発生しました（$detail）"
    }
