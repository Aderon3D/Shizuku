package moe.shizuku.manager.intents.models

sealed interface TokenValidationError {
    data object TokenRequired : TokenValidationError
    data object TokenInvalid : TokenValidationError
}