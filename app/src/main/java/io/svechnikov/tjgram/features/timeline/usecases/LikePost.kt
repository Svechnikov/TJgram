package io.svechnikov.tjgram.features.timeline.usecases

import io.svechnikov.tjgram.base.UseCase
import io.svechnikov.tjgram.base.db.BaseDatabase
import io.svechnikov.tjgram.base.exceptions.NotAuthenticatedException
import io.svechnikov.tjgram.base.network.Gateway
import javax.inject.Inject

class LikePost @Inject constructor(
    private val db: BaseDatabase,
    private val gateway: Gateway
): UseCase<LikePost.Params, Unit>() {

    override suspend fun run(params: Params) {
        db.user().user() ?: throw NotAuthenticatedException()

        var like = params.like

        if (params.isLiked == params.like) {
            like = 0
        }

        val result = gateway.likePost(params.postId, like)

        db.posts().updateLikes(params.postId, result.likes, result.isLiked)
    }

    data class Params(val postId: Long, val isLiked: Int, val like: Int)
}