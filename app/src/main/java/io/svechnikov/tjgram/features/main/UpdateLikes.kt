package io.svechnikov.tjgram.features.main

import io.svechnikov.tjgram.base.UseCase
import io.svechnikov.tjgram.base.data.WebSocketMessage
import io.svechnikov.tjgram.base.db.BaseDatabase
import javax.inject.Inject

class UpdateLikes @Inject constructor(
    private val db: BaseDatabase
): UseCase<WebSocketMessage, Unit>() {

    override suspend fun run(params: WebSocketMessage) {
        var isLiked = 0

        if (db.user().user()?.userHash == params.userHash) {
            isLiked = params.state
        }

        db.posts().updateLikes(params.id, params.count, isLiked)
    }
}