package io.svechnikov.tjgram.features.timeline.child

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.REPEAT_MODE_ALL
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.squareup.picasso.Picasso
import io.svechnikov.tjgram.R
import io.svechnikov.tjgram.databinding.PostItemBinding
import timber.log.Timber
import javax.inject.Inject


class PostsAdapter(private val likesListener: LikesListener,
                   private val picasso: Picasso,
                   private val videoPlayer: ExoPlayer,
                   private val mediaSourceFactory: ExtractorMediaSource.Factory) :
    PagedListAdapter<PostView, PostsAdapter.PostViewHolder>(
        DIFF_CALLBACK
    ) {

    private var width = 0
    private var currentPlayingHolder: PostViewHolder? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = PostItemBinding.inflate(inflater, parent, false)

        width = parent.measuredWidth

        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.post = getItem(position)
    }

    override fun onViewRecycled(holder: PostViewHolder) {
        holder.clear()
        super.onViewRecycled(holder)
    }

    /**
     * 0. При воспроизведении видео отдаём предпочтение тем, которые больше видно на экране
     * 1. Если на экране появилось видео, воспроизводим его только если пользователь
     * скроллит список, не отпуская пальца от экрана, либо если скролл закончился
     * 2. Если на экране появилось видео в тот момент, когда уже воспроизводилось другое видео и другое
     * видео не ушло за границы экрана, ничего не делаем
     * 3. Если видео ушло за границы экрана, перестаём его воспроизводить
     */
    fun checkPlayback(orderedVisibleHolders: List<PostViewHolder>,
                      isSettling: Boolean) {
        if (currentPlayingHolder == null && !orderedVisibleHolders.isEmpty()) {
            if (!isSettling) {
                orderedVisibleHolders[0].let {
                    currentPlayingHolder = it
                    it.playVideo()
                }
            }
        }
        else if (currentPlayingHolder != null && orderedVisibleHolders.isEmpty()) {
            currentPlayingHolder?.pauseVideo()
            currentPlayingHolder = null
        }
        else if (currentPlayingHolder != null && !orderedVisibleHolders.isEmpty()) {
            currentPlayingHolder?.let {
                if (!orderedVisibleHolders.contains(it)) {
                    currentPlayingHolder?.pauseVideo()
                    if (!isSettling) {
                        currentPlayingHolder = orderedVisibleHolders[0]
                        currentPlayingHolder?.playVideo()
                    }
                }
            }
        }
    }

    fun pausePlaybackIfNecessary() {
        currentPlayingHolder?.pauseVideo()
    }

    fun resumePlaybackIfNecessary() {
        currentPlayingHolder?.playVideo()
    }

    companion object {
        private val DIFF_CALLBACK = object: DiffUtil.ItemCallback<PostView>() {
            override fun areItemsTheSame(oldItem: PostView,
                                         newItem: PostView
            ): Boolean {
                return oldItem.postId == newItem.postId
            }

            override fun areContentsTheSame(oldItem: PostView,
                                            newItem: PostView
            ): Boolean {
                return oldItem.postId == newItem.postId &&
                        oldItem.likes == newItem.likes &&
                        oldItem.isLiked == newItem.isLiked
            }
        }
    }

    class Factory @Inject constructor(private val picasso: Picasso,
                                      private val player: ExoPlayer,
                                      private val mediaSourceFactory: ExtractorMediaSource.Factory) {
        fun create(likesListener: LikesListener): PostsAdapter {
            return PostsAdapter(
                likesListener,
                picasso,
                player,
                mediaSourceFactory
            )
        }
    }

    inner class PostViewHolder(val binding: PostItemBinding) :
        RecyclerView.ViewHolder(binding.root), Player.EventListener {

        private var isPlaying = false

        var post: PostView? = null
            set(item) {
                binding.post = item

                item?.let {
                    val height = (width / it.mediaRatio).toInt()

                    binding.mediaWrapper.layoutParams.width = width
                    binding.mediaWrapper.layoutParams.height = height
                    binding.mediaWrapper.requestLayout()

                    picasso.load(it.userAvatarUrl)
                        .resizeDimen(
                            R.dimen.timeline_post_avatar_size,
                            R.dimen.timeline_post_avatar_size)
                        .into(binding.avatar)
                }

                binding.root.tag = this

                binding.executePendingBindings()

                field = item
            }

        init {
            binding.likeMinus.setOnClickListener {
                post?.let {
                    likesListener.onMinus(it)
                }
            }
            binding.likePlus.setOnClickListener {
                post?.let {
                    likesListener.onPlus(it)
                }
            }
        }

        fun isVideo(): Boolean {
            return post?.isVideo() ?: false
        }

        fun playVideo() {
            if (!isPlaying) {
                Timber.i("play ${post?.userName}")
                isPlaying = true

                videoPlayer.apply {
                    post?.let {
                        repeatMode = REPEAT_MODE_ALL
                        addListener(this@PostViewHolder)
                        videoComponent?.setVideoTextureView(binding.playerView)

                        playWhenReady = true

                        val mediaSource = mediaSourceFactory
                            .createMediaSource(Uri.parse(it.videoUrl))

                        prepare(mediaSource)

                        seekTo(it.videoPosition)
                    }
                }
            }
        }

        fun pauseVideo() {
            if (isPlaying) {
                Timber.i("pause ${post?.userName}")
                post?.videoPosition = videoPlayer.currentPosition
                isPlaying = false
                videoPlayer.playWhenReady = false
                videoPlayer.removeListener(this)
            }
        }

        fun clear() {
            if (isVideo()) {
                pauseVideo()
                if (this == currentPlayingHolder) {
                    currentPlayingHolder = null
                }
                binding.playerView.visibility = View.GONE
            }
            binding.root.tag = null
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean,
                                          playbackState: Int) {
            binding.playerView.visibility = when(playbackState) {
                Player.STATE_READY -> {
                    View.VISIBLE
                }
                else -> {
                    View.GONE
                }
            }
        }

        override fun onPlayerError(error: ExoPlaybackException?) {
            Timber.i("onPlayerError")
            Timber.e(error)
        }
    }

    interface LikesListener {
        fun onMinus(post: PostView)
        fun onPlus(post: PostView)
    }
}