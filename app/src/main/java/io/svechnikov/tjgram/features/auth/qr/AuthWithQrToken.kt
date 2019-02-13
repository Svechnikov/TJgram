package io.svechnikov.tjgram.features.auth.qr

import io.svechnikov.tjgram.base.UseCase
import io.svechnikov.tjgram.base.db.BaseDatabase
import io.svechnikov.tjgram.base.network.Gateway
import timber.log.Timber
import javax.inject.Inject

class AuthWithQrToken @Inject constructor(
    private val gateway: Gateway,
    private val db: BaseDatabase
): UseCase<AuthWithQrToken.Params, Unit>() {

    override suspend fun run(params: AuthWithQrToken.Params) {
        val user = gateway.authQr(params.token.replaceFirst("v3|", ""))
        Timber.i("user: $user")

        db.runInTransaction {
            db.user().insert(user)
            db.posts().clearAll()
        }
    }

    data class Params(val token: String)
}