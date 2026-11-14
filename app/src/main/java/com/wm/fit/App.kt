package com.wm.fit

import android.app.Application
import com.wm.fit.core.Diary
import com.wm.fit.core.P
import com.wm.fit.core.RestTimer
import com.wm.fit.core.Secrets
import com.wm.fit.core.Store

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        P.init(this)
        Store.load()
        Diary.photoDir = java.io.File(filesDir, "photos").apply { mkdirs() }
        Diary.load()
        RestTimer.channels(this)
        Secrets.init(this)
    }
}
