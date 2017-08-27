package com.epishie.news.features

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.epishie.news.R
import com.epishie.news.component
import com.epishie.news.features.common.inflate
import com.epishie.news.features.sources.SourcesAdapter
import com.epishie.news.features.sources.SourcesViewModel
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.sources_fragment.*
import kotlinx.android.synthetic.main.sources_fragment.view.*

class SourcesFragment : BottomSheetDialogFragment() {
    lateinit var vm: SourcesViewModel
    lateinit var disposable: Disposable
    lateinit var adapter: SourcesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)
        vm = ViewModelProviders.of(this, component.vmFactory())[SourcesViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return container.inflate(inflater, R.layout.sources_fragment, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = SourcesAdapter()
        view.sourceList.adapter = adapter
        view.sourceList.layoutManager = LinearLayoutManager(context,
                LinearLayoutManager.VERTICAL, false)
    }

    override fun onStart() {
        super.onStart()
        disposable = vm.update(Flowable.just(SourcesViewModel.Event.Refresh))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { state ->
                    progress.visibility = if (state.progress) View.VISIBLE else View.GONE
                    adapter.sources = state.sources
                }
    }

    override fun onStop() {
        disposable.dispose()
        super.onStop()
    }
}