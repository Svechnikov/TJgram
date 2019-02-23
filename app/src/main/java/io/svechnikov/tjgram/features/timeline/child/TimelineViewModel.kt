package io.svechnikov.tjgram.features.timeline.child

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.switchMap
import androidx.lifecycle.ViewModel
import androidx.paging.Config
import androidx.paging.PagedList
import androidx.paging.toLiveData
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import io.svechnikov.tjgram.R
import io.svechnikov.tjgram.base.SingleLiveEvent
import io.svechnikov.tjgram.base.db.BaseDatabase
import io.svechnikov.tjgram.base.db.entities.Post
import io.svechnikov.tjgram.base.exceptions.NoConnectionException
import io.svechnikov.tjgram.base.exceptions.NotAuthenticatedException
import io.svechnikov.tjgram.base.exceptions.ServiceException
import io.svechnikov.tjgram.base.schedulers.Executors
import io.svechnikov.tjgram.base.schedulers.Schedulers
import io.svechnikov.tjgram.base.utils.PagingRequestHelper
import io.svechnikov.tjgram.features.timeline.child.usecases.LikePost
import io.svechnikov.tjgram.features.timeline.child.usecases.QueryTimeline
import io.svechnikov.tjgram.features.timeline.child.usecases.RefreshTimeline
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class TimelineViewModel @Inject constructor(
    private val db: BaseDatabase,
    private val queryTimeline: QueryTimeline,
    private val refreshTimeline: RefreshTimeline,
    private val likePost: LikePost,
    private val context: Context,
    schedulers: Schedulers,
    executors: Executors
) : ViewModel() {

    companion object {
        const val PAGE_SIZE = 10
    }

    private val sortingMutable = MutableLiveData<Post.Sorting>()
    private val stateMutable = MutableLiveData<TimelineState>()
    private val eventMutable = SingleLiveEvent<TimelineEvent>()

    private val scrollSubject = PublishSubject.create<Unit>()
    private val scrollDisposable: Disposable

    val posts: LiveData<PagedList<PostView>> = switchMap(sortingMutable) {
        stateMutable.value = TimelineState.Loading
        db.posts().posts(it).map {entity ->
            stateMutable.postValue(TimelineState.Loaded)

            var intro: CharSequence? = null

            if (!entity.intro.isEmpty()) {
                intro = IntroCharSequence(entity.userName, entity.intro)
            }

            PostView(
                postId = entity.postId,
                title = entity.title,
                intro = intro,
                userName = entity.userName,
                userId = entity.userId,
                userAvatarUrl = entity.userAvatarUrl,
                thumbnailUrl = entity.thumbnailUrl!!,
                mediaRatio = entity.mediaRatio,
                videoUrl = entity.videoUrl,
                isLiked = entity.isLiked,
                likes = LikesCharSequence(context, entity.likes)
            )
        }.toLiveData(
            config = Config(
                pageSize = PAGE_SIZE,
                enablePlaceholders = true
            ),
            boundaryCallback = TimelineBoundaryCallback(
                queryTimeline,
                PAGE_SIZE, it, executors
            ).apply {
                helper.addListener {
                    if (it.hasRunning()) {
                        stateMutable.postValue(TimelineState.Loading)
                    }
                    else if (it.hasError()) {
                        var error = it.getErrorFor(PagingRequestHelper.RequestType.INITIAL)

                        if (error == null) {
                            error = it.getErrorFor(PagingRequestHelper.RequestType.AFTER)
                        }

                        val message = getErrorMessage(error ?: IOException())
                        val event = TimelineEvent.ShowError(message)

                        eventMutable.postValue(event)
                    }
                    else {
                        stateMutable.postValue(TimelineState.Loaded)
                    }
                }
            }
        )
    }

    val event: LiveData<TimelineEvent> = eventMutable

    val state: LiveData<TimelineState> = stateMutable

    init {
        // не стал придумывать велосипед на LiveData и сделал при помощи RxJava
        scrollDisposable = scrollSubject.sample(500, TimeUnit.MILLISECONDS)
            .observeOn(schedulers.main())
            .subscribeOn(schedulers.io())
            .subscribe {
                eventMutable.value = TimelineEvent.Scroll
            }
    }

    override fun onCleared() {
        super.onCleared()

        scrollDisposable.dispose()
    }

    fun setSorting(sorting: Post.Sorting) {
        if (sortingMutable.value != sorting) {
            sortingMutable.value = sorting
        }
    }

    fun refresh() {
        sortingMutable.value?.let {
            stateMutable.value = TimelineState.Refreshing
            refreshTimeline(
                RefreshTimeline.Params(it,
                    PAGE_SIZE
                )) { result ->
                result.either(::onError) {
                    stateMutable.postValue(TimelineState.Loaded)
                }
            }
        }
    }

    fun likePostMinus(post: PostView) {
        likePost(post, -1)
    }

    fun likePostPlus(post: PostView) {
        likePost(post, 1)
    }

    fun onScroll() {
        scrollSubject.onNext(Unit)
    }

    private fun likePost(post: PostView, like: Int) {
        likePost(LikePost.Params(post.postId, post.isLiked, like)) {
            it.either(::onLikeError) {}
        }
    }

    private fun onLikeError(e: Throwable) {
        when (e) {
            is NotAuthenticatedException -> {
                eventMutable.value = TimelineEvent.NavigateToAuth
            }
            else -> onError(e)
        }
    }

    private fun getErrorMessage(throwable: Throwable): String {
        return when(throwable) {
            is NoConnectionException -> {
                context.getString(R.string.no_connection_error)
            }
            is ServiceException -> {
                throwable.serviceMessage
            }
            else -> {
                context.getString(R.string.generic_error)
            }
        }
    }

    private fun onError(throwable: Throwable) {
        stateMutable.value = TimelineState.Loaded
        eventMutable.value =
            TimelineEvent.ShowError(getErrorMessage(throwable))
    }
}

sealed class TimelineEvent {
    data class ShowError(val message: String) : TimelineEvent()
    object NavigateToAuth : TimelineEvent()
    object Scroll : TimelineEvent()
}

sealed class TimelineState {
    object Refreshing : TimelineState()
    object Loading : TimelineState()
    object Loaded : TimelineState()
}

