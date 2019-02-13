package io.svechnikov.tjgram.features.timeline.usecases

import io.svechnikov.tjgram.base.UseCase
import io.svechnikov.tjgram.base.db.BaseDatabase
import io.svechnikov.tjgram.base.db.entities.Post
import io.svechnikov.tjgram.base.network.Gateway
import javax.inject.Inject

class RefreshTimeline @Inject constructor(
    private val db: BaseDatabase,
    private val gateway: Gateway
): UseCase<RefreshTimeline.Params, Unit>() {

    override suspend fun run(params: Params) {
        val posts = gateway.getTimeline(params.sorting, params.pageSize, 0)

        db.runInTransaction {
            db.posts().clear(params.sorting)
            db.posts().insert(posts.map {
                it.copy(sorting = params.sorting)
            })
        }
    }

    data class Params(val sorting: Post.Sorting,
                      val pageSize: Int)
}
