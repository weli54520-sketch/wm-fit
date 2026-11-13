package com.wm.fit

import android.app.Application
import com.wm.fit.core.P

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        P.init(this)
    }
}
