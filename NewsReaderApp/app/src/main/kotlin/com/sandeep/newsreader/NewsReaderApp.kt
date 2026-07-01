package com.sandeep.newsreader

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// @HiltAndroidApp triggers Hilt's code generation.
// Every module in the app that needs DI inherits from this component.
@HiltAndroidApp
class NewsReaderApp : Application()
