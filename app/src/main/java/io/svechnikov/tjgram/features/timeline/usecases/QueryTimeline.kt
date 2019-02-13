package io.svechnikov.tjgram.features.timeline.usecases

import io.svechnikov.tjgram.base.UseCase
import io.svechnikov.tjgram.base.db.BaseDatabase
import io.svechnikov.tjgram.base.db.entities.Post
import io.svechnikov.tjgram.base.network.Gateway
import javax.inject.Inject

class QueryTimeline @Inject constructor(
    private val db: BaseDatabase,
    private val gateway: Gateway
): UseCase<QueryTimeline.Params, Unit>() {

    override suspend fun run(params: Params) {
        if (db.posts().hasFinalPost(params.sorting)) {
            return
        }

        db.runInTransaction {
            val offset = db.posts().count(params.sorting)

            val posts = gateway.getTimeline(params.sorting, params.pageSize, offset)

            db.posts().insert(posts.map {
                it.copy(sorting = params.sorting)
            })

            if (posts.size < params.pageSize) {
                db.posts().setFinal(db.posts().getLastId())
            }
        }
    }

    data class Params(val pageSize: Int,
                      val sorting: Post.Sorting)
}

