package com.epishie.news.features.sources

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
import com.jakewharton.rxbinding2.view.visibility
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.sources_fragment.*
import kotlinx.android.synthetic.main.sources_fragment.view.*
import javax.inject.Inject
import javax.inject.Named

class SourcesFragment : BottomSheetDialogFragment() {
    @field:[Inject Named("ui")]
    lateinit var ui: Scheduler
    private lateinit var vm: SourcesViewModel
    private lateinit var adapter: SourcesAdapter
    private lateinit var disposable: Disposable

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
        val selections = adapter.selections.map { source ->
            SourcesViewModel.Event.Select(source)
        }
        val events = Flowable.merge(
                Flowable.just(SourcesViewModel.Event.Refresh),
                selections
        )

        val states = vm.update(events)
                .observeOn(ui)
                .publish()
        states.map { (progress) -> progress }
                .subscribe(progress.visibility())
        states.map { state -> state.sources }
                .subscribe(adapter.sources)

        disposable = states.connect()
    }

    override fun onStop() {
        disposable.dispose()
        super.onStop()
    }
}