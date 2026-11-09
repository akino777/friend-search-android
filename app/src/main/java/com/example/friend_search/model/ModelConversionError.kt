package com.example.friend_search.model

/** APIModel から Model への変換時の契約違反 */
sealed class ModelConversionError : Exception() {
    data class UnknownVCTool(val value: String) : ModelConversionError()
    data class UnknownPostStatus(val value: String) : ModelConversionError()
    data class InvalidDate(val value: String) : ModelConversionError()
}
