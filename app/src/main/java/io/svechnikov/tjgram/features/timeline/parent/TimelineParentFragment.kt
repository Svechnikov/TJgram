package io.svechnikov.tjgram.features.timeline.parent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.viewpager.widget.PagerAdapter
import com.google.android.material.tabs.TabLayout
import io.svechnikov.tjgram.R
import io.svechnikov.tjgram.features.main.MainViewModel
import io.svechnikov.tjgram.base.db.entities.Post
import io.svechnikov.tjgram.base.di.Injectable
import io.svechnikov.tjgram.databinding.FragmentTimelineParentBinding
import io.svechnikov.tjgram.features.timeline.child.TimelineFragment
import javax.inject.Inject

class TimelineParentFragment: Fragment(), Injectable {

    private lateinit var binding: FragmentTimelineParentBinding

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: TimelineParentViewModel

    private var pagerAdapter: PagerAdapter? = null

    companion object {
        const val TAB_POSITION_NEW = 0
        const val TAB_POSITION_WEEK = 1
        const val TAB_POSITION_MONTH = 2
        const val TAB_POSITION_YEAR = 3
    }

    fun navController() = findNavController()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentTimelineParentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val mainViewModel = ViewModelProviders.of(
            activity!!, viewModelFactory)[MainViewModel::class.java]

        mainViewModel.setBottomBarVisibility(true)
        mainViewModel.setToolbarScrollable(true)

        viewModel = ViewModelProviders.of(
            this, viewModelFactory)[TimelineParentViewModel::class.java]

        viewModel.state.observe(viewLifecycleOwner, Observer {
            if (it is TimelineParentState.ShowPage) {
                binding.pager.currentItem = when(it.sorting) {
                    Post.Sorting.NEW -> {
                        TAB_POSITION_NEW
                    }
                    Post.Sorting.TOP_WEEK -> {
                        TAB_POSITION_WEEK
                    }
                    Post.Sorting.TOP_MONTH -> {
                        TAB_POSITION_MONTH
                    }
                    Post.Sorting.TOP_YEAR -> {
                        TAB_POSITION_YEAR
                    }
                    else -> {
                        throw IllegalArgumentException()
                    }
                }
            }
        })

        viewModel.event.observe(viewLifecycleOwner, Observer {
            when(it) {
                TimelineParentEvent.NavigateToAuth -> {
                    navController().navigate(
                        R.id.action_timelineParentFragment_to_selectAuthMethodFragment)
                }
            }
        })

        initViews()
    }

    private fun initViews() {
        if (pagerAdapter == null) {
            pagerAdapter = TimelinePagerAdapter(childFragmentManager)
        }
        binding.pager.adapter = pagerAdapter
        binding.pager.offscreenPageLimit = 3
        binding.tabs.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewModel.selectSorting(getSortingByPosition(tab.position))
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {

            }
        })
        binding.pager.addOnPageChangeListener(
            TabLayout.TabLayoutOnPageChangeListener(binding.tabs))
    }

    private fun getSortingByPosition(position: Int): Post.Sorting {
        return when(position) {
            TAB_POSITION_NEW -> Post.Sorting.NEW
            TAB_POSITION_WEEK -> Post.Sorting.TOP_WEEK
            TAB_POSITION_MONTH -> Post.Sorting.TOP_MONTH
            TAB_POSITION_YEAR -> Post.Sorting.TOP_YEAR
            else -> throw IllegalArgumentException()
        }
    }

    private inner class TimelinePagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
        override fun getItem(position: Int): Fragment {
            val sorting = getSortingByPosition(position)

            return TimelineFragment.newInstance(sorting)
        }

        override fun getCount(): Int {
            return 4
        }
    }
}