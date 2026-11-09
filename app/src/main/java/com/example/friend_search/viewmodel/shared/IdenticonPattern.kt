package com.example.friend_search.viewmodel.shared

import java.security.MessageDigest

/**
 * GitHub 風の 5×5 左右対称 Identicon の表示パターン。iOS / Android で同じ結果になる共通仕様。
 *
 * 1. 入力は `"$avatarVersion:${publicUserId.lowercase()}"` の UTF-8
 * 2. SHA-256 の 32 バイト digest
 * 3. digest 先頭から 15 ビットを行優先（5行×3列）で読む。各バイトは最上位ビットから
 * 4. 右2列は左2列を左右反転コピー
 * 5. 配色は `digest[15] % 8` で固定パレットから選ぶ
 */
data class IdenticonPattern(
    /** cells[row][column]（true = 前景色） */
    val cells: List<List<Boolean>>,
    val palette: IdenticonPalette,
) {
    companion object {
        const val SIZE = 5

        fun of(publicUserId: String, avatarVersion: Int): IdenticonPattern {
            val input = "$avatarVersion:${publicUserId.lowercase()}"
            return of(MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8)))
        }

        private fun of(digest: ByteArray): IdenticonPattern {
            require(digest.size == 32) { "SHA-256 digest must be 32 bytes" }
            fun byte(index: Int) = digest[index].toInt() and 0xFF
            fun bit(index: Int) = (byte(index / 8) shr (7 - index % 8)) and 1 == 1
            val rows = (0 until SIZE).map { row ->
                val left = (0 until 3).map { bit(row * 3 + it) }
                listOf(left[0], left[1], left[2], left[1], left[0])
            }
            return IdenticonPattern(rows, IdenticonPalette.all[byte(15) % 8])
        }
    }
}

/** 背景 / 前景（16進RGB）。値は共通仕様として固定する。 */
data class IdenticonPalette(
    val background: Int,
    val foreground: Int,
) {
    companion object {
        val all = listOf(
            IdenticonPalette(0xEEF2FF, 0x3730A3),
            IdenticonPalette(0xECFDF5, 0x065F46),
            IdenticonPalette(0xFFF7ED, 0x9A3412),
            IdenticonPalette(0xFDF2F8, 0x9D174D),
            IdenticonPalette(0xEFF6FF, 0x1D4ED8),
            IdenticonPalette(0xF0FDFA, 0x0F766E),
            IdenticonPalette(0xFEFCE8, 0x854D0E),
            IdenticonPalette(0xF5F3FF, 0x6D28D9),
        )
    }
}
