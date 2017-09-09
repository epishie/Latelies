package com.epishie.news.features.story

import android.arch.lifecycle.ViewModel
import com.epishie.news.model.StoryModel
import javax.inject.Inject


class StoryViewModel
@Inject constructor(private val storyModel: StoryModel) : ViewModel() {
}