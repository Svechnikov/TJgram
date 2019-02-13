package io.svechnikov.tjgram.features.timeline

import androidx.paging.PagedList
import io.reactivex.schedulers.Schedulers
import io.svechnikov.tjgram.base.db.entities.Post
import io.svechnikov.tjgram.base.schedulers.Executors
import io.svechnikov.tjgram.base.utils.PagingRequestHelper
import io.svechnikov.tjgram.features.timeline.usecases.QueryTimeline

class TimelineBoundaryCallback(
    private val queryTimeline: QueryTimeline,
    private val pageSize: Int,
    private val sorting: Post.Sorting,
    executors: Executors
) : PagedList.BoundaryCallback<PostView>() {

    val helper = PagingRequestHelper(executors.newSingleThreadExecutor())

    override fun onZeroItemsLoaded() {
        Schedulers.io()
        helper.runIfNotRunning(PagingRequestHelper.RequestType.INITIAL) { callback ->
            queryTimeline(QueryTimeline.Params(pageSize, sorting)) {
                it.either({error ->
                    callback.recordFailure(error)
                }, {
                    callback.recordSuccess()
                })
            }
        }
    }

    override fun onItemAtEndLoaded(itemAtEnd: PostView) {
        helper.runIfNotRunning(PagingRequestHelper.RequestType.AFTER) { callback ->
            queryTimeline(
                QueryTimeline.Params(
                    pageSize,
                    sorting
                )
            ) {
                it.either({error ->
                    callback.recordFailure(error)
                }, {
                    callback.recordSuccess()
                })
            }
        }
    }
}