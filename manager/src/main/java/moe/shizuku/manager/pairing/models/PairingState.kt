package moe.shizuku.manager.pairing.models

sealed class PairingState {
    object Searching : PairingState()
    data class Ready(val port: Int) : PairingState()
    data class Working(val port: Int, val code: String) : PairingState()
    object Success : PairingState()
    data class Failure(val message: String) : PairingState()
}