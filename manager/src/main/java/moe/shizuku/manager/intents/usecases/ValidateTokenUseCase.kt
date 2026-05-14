package moe.shizuku.manager.intents.usecases

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import moe.shizuku.manager.intents.data.TokenRepository
import moe.shizuku.manager.intents.models.TokenValidationError

class ValidateTokenUseCase(
    private val tokenRepository: TokenRepository
) {
    operator fun invoke(token: String?): Result<Unit, TokenValidationError> {
        val expectedToken = tokenRepository.getAuthToken()

        return if (token.isNullOrEmpty()) {
            Err(TokenValidationError.TokenRequired)
        } else if (token != expectedToken) {
            Err(TokenValidationError.TokenInvalid)
        } else {
            Ok(Unit)
        }
    }
}