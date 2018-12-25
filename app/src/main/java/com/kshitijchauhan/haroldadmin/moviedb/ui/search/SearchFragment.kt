package com.kshitijchauhan.haroldadmin.moviedb.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.*
import com.jakewharton.rxbinding2.internal.Notification
import com.jakewharton.rxbinding2.widget.RxTextView
import com.jakewharton.rxrelay2.PublishRelay
import com.kshitijchauhan.haroldadmin.moviedb.R
import com.kshitijchauhan.haroldadmin.moviedb.R.id.*
import com.kshitijchauhan.haroldadmin.moviedb.ui.BaseFragment
import com.kshitijchauhan.haroldadmin.moviedb.ui.UIState
import com.kshitijchauhan.haroldadmin.moviedb.ui.main.MainViewModel
import com.kshitijchauhan.haroldadmin.moviedb.utils.gone
import com.kshitijchauhan.haroldadmin.moviedb.utils.hideKeyboard
import com.kshitijchauhan.haroldadmin.moviedb.utils.visible
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_search.*
import java.util.concurrent.TimeUnit

class SearchFragment : BaseFragment() {

    override val associatedState: UIState = UIState.SearchScreenState

    companion object {
        fun newInstance() = SearchFragment()
    }

    private val onPauseRelay: PublishRelay<Any> = PublishRelay.create()

    private lateinit var mainViewModel: MainViewModel
    private lateinit var searchViewModel: SearchViewModel
    private lateinit var searchAdapter: SearchResultsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        searchViewModel = ViewModelProviders.of(this).get(SearchViewModel::class.java)
        mainViewModel = ViewModelProviders.of(activity!!).get(MainViewModel::class.java)

        searchViewModel.searchUpdate.observe(viewLifecycleOwner, Observer {
            searchAdapter.updateList(it)
            if (it.first.isEmpty()) {
                TransitionManager.beginDelayedTransition(searchRootView)
                rvSearchResults.gone()
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchAdapter = SearchResultsAdapter(mutableListOf())
        rvSearchResults.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = searchAdapter
            itemAnimator = SlideDownAlphaAnimator()
        }

        etSearchBox.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                val set = TransitionSet()
                set.apply {
                    addTransition(
                        ChangeBounds()
                            .addTarget(R.id.tvTitleSearch)
                            .addTarget(R.id.searchCard)
                    )
                }
                TransitionManager.beginDelayedTransition(searchRootView, set)
                tvTitleSearch.gone()
            } else {
                val set = TransitionSet()
                set.apply {
                    addTransition(
                        ChangeBounds()
                            .addTarget(R.id.tvTitleSearch)
                            .addTarget(R.id.searchCard)
                            .addTarget(R.id.rvSearchResults)
                    )
                }
                TransitionManager.beginDelayedTransition(searchRootView, set)
                tvTitleSearch.visible()
                rvSearchResults.gone()
            }
        }

        searchIcon.setOnClickListener {
            etSearchBox.clearFocus()
            searchRootView.requestFocus()
            activity?.hideKeyboard()
        }

        subscribeToQueries()
    }

    override fun onPause() {
        super.onPause()
        onPauseRelay.accept(Notification.INSTANCE)
    }

    private fun subscribeToQueries() {
        RxTextView.textChangeEvents(etSearchBox)
            .debounce(400, TimeUnit.MILLISECONDS)
            .map { event ->
                event.text().toString()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { query ->
                if (query.length > 2) {
                    searchViewModel.getMoviesForQuery(query)
                    TransitionManager.beginDelayedTransition(searchRootView)
                    rvSearchResults.visible()
                } else {
                    TransitionManager.beginDelayedTransition(searchRootView)
                    rvSearchResults.gone()
                }
            }
            .takeUntil(onPauseRelay)
            .subscribe()
    }
}
