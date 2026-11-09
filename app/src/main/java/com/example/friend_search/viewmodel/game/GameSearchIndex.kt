package com.example.friend_search.viewmodel.game

import com.example.friend_search.model.Game
import java.text.Normalizer

/**
 * 端末内のゲーム候補検索（AI は使わない）。表示名・別名を正規化して照合し、前方一致を部分一致より優先する。
 * 正規化した名前は作成時に一度だけ求め、入力のたびに全ゲームを正規化し直さない。
 */
class GameSearchIndex(games: List<Game>) {
    private val entries: List<Pair<Game, List<String>>> = games.filter(Game::isActive).map { game ->
        game to (listOf(game.displayName) + game.aliases).map(::normalize)
    }

    /** 入力が空なら有効なゲームをすべて返す */
    fun search(query: String): List<Game> {
        val needle = normalize(query)
        if (needle.isEmpty()) return entries.map { it.first }
        val prefix = mutableListOf<Game>()
        val contains = mutableListOf<Game>()
        for ((game, names) in entries) {
            when {
                names.any { it.startsWith(needle) } -> prefix += game
                names.any { it.contains(needle) } -> contains += game
            }
        }
        return prefix + contains
    }

    companion object {
        /** NFKC・ひらがな→カタカナ・小文字化・前後空白の除去・連続空白の圧縮 */
        fun normalize(value: String): String {
            val nfkc = Normalizer.normalize(value, Normalizer.Form.NFKC)
            return hiraganaToKatakana(nfkc)
                .lowercase()
                .split(Regex("\\s+"))
                .filter(String::isNotEmpty)
                .joinToString(" ")
        }

        /** ひらがな（U+3041〜U+3096・U+309D〜U+309E）を対応するカタカナ（+0x60）にする */
        private fun hiraganaToKatakana(value: String): String = buildString(value.length) {
            for (char in value) {
                append(if (char in 'ぁ'..'ゖ' || char in 'ゝ'..'ゞ') char + 0x60 else char)
            }
        }
    }
}
