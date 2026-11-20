package com.waleed.onyx

import android.app.Application
import com.waleed.onyx.apps.AppCatalog
import com.waleed.onyx.data.HomeRepository
import com.waleed.onyx.data.OnyxDb
import com.waleed.onyx.data.Settings

/** الأشياء اللي تعيش طول ما اللانشر شغال */
class OnyxApp : Application() {
    lateinit var settings: Settings
    lateinit var catalog: AppCatalog
    lateinit var repo: HomeRepository

    override fun onCreate() {
        super.onCreate()
        settings = Settings(this)
        catalog = AppCatalog(this)
        repo = HomeRepository(OnyxDb.create(this).items(), catalog, settings)
    }
}
