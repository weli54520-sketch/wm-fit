package com.wm.fit

import android.app.Application
import com.wm.fit.core.P
import com.wm.fit.core.RestTimer
import com.wm.fit.core.Store

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        P.init(this)
        Store.load()
        RestTimer.channels(this)
    }
}
