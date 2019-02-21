package io.svechnikov.tjgram.features.timeline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.svechnikov.tjgram.R
import io.svechnikov.tjgram.base.MainEvent
import io.svechnikov.tjgram.base.MainViewModel
import io.svechnikov.tjgram.base.db.entities.Post
import io.svechnikov.tjgram.base.di.Injectable
import io.svechnikov.tjgram.databinding.FragmentTimelineBinding
import javax.inject.Inject


class TimelineFragment : Fragment(), Injectable {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var postsAdapterFactory: PostsAdapter.Factory

    private lateinit var postsAdapter: PostsAdapter

    private lateinit var layoutManager: LinearLayoutManager

    private lateinit var viewModel: TimelineViewModel

    private lateinit var binding: FragmentTimelineBinding

    private var wasRefreshing = false

    private var checkedPlaybackOnViewCreate = false

    fun navController() = findNavController()

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentTimelineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        checkedPlaybackOnViewCreate = false

        viewModel = ViewModelProviders.of(
            this, viewModelFactory)[TimelineViewModel::class.java]

        val mainViewModel = ViewModelProviders.of(
            activity!!, viewModelFactory)[MainViewModel::class.java]

        mainViewModel.setBottomBarVisibility(true)

        initViews()

        viewModel.setSorting(Post.Sorting.NEW)

        viewModel.posts.observe(viewLifecycleOwner, Observer {posts ->
            (binding.posts.adapter as PostsAdapter).submitList(posts)
            if (wasRefreshing) {
                wasRefreshing = false
                binding.posts.post {
                    binding.posts.scrollToPosition(0)
                }
            }
        })
        viewModel.state.observe(viewLifecycleOwner, Observer {state ->
            binding.refresh.isRefreshing = state == TimelineState.Refreshing

            if (state == TimelineState.Loaded && !checkedPlaybackOnViewCreate) {
                checkedPlaybackOnViewCreate = true
                binding.root.post {
                    checkPlayback()
                }
            }
        })

        viewModel.event.observe(viewLifecycleOwner, Observer {
            when(it) {
                is TimelineEvent.ShowError -> {
                    mainViewModel.setEvent(MainEvent.ShowMessage(it.message))
                }
                TimelineEvent.NavigateToAuth -> {
                    navController().navigate(
                        R.id.action_timelineFragment_to_selectAuthMethodFragment)
                }
                TimelineEvent.Scroll -> {
                    checkPlayback()
                }
            }
        })
    }

    override fun onPause() {
        super.onPause()

        postsAdapter.pausePlaybackIfNecessary()
    }

    override fun onResume() {
        super.onResume()

        postsAdapter.resumePlaybackIfNecessary()
    }

    private fun initViews() {
        binding.posts.setHasFixedSize(false)
        binding.posts.itemAnimator = null
        postsAdapter = postsAdapterFactory.create(object: PostsAdapter.LikesListener {
            override fun onMinus(post: PostView) {
                viewModel.likePostMinus(post)
            }

            override fun onPlus(post: PostView) {
                viewModel.likePostPlus(post)
            }
        })
        binding.posts.adapter = postsAdapter
        layoutManager = binding.posts.layoutManager as LinearLayoutManager
        binding.refresh.setOnRefreshListener {
            wasRefreshing = true
            viewModel.refresh()
        }
        binding.posts.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                viewModel.onScroll()
            }
        })
    }

    /**
     * Собираем список видео, в котором каждое видео занимает
     * как минимум 10% видимой части экрана, сортируем его по
     * убыванию видимой высоты и отправляем адаптеру
     */
    private fun checkPlayback() {
        val recyclerView = binding.posts
        val recyclerHeight = recyclerView.height
        val minItemVisibleHeight = recyclerHeight * 0.1

        val visibleItems = hashMapOf<Int, PostsAdapter.PostViewHolder>()
        for (i in 0..recyclerView.childCount) {
            val view = recyclerView.getChildAt(i) ?: continue
            val holder = view.tag as PostsAdapter.PostViewHolder

            if (!holder.isVideo()) {
                continue
            }

            val binding = holder.binding

            val top = view.top + binding.mediaWrapper.top
            val bottom = top + binding.mediaWrapper.height
            val visibleHeight = Math.min(bottom, recyclerHeight) - Math.max(top, 0)

            // at least 10% of recycler view
            if (visibleHeight >= minItemVisibleHeight) {
                visibleItems[visibleHeight] = holder
            }
        }

        val sortedItems = visibleItems.map { it.key }
            .sortedDescending()
            .map { visibleItems[it] }
            .filterNotNull()

        val isSettling = recyclerView.scrollState == RecyclerView.SCROLL_STATE_SETTLING
        postsAdapter.checkPlayback(sortedItems, isSettling)
    }
}