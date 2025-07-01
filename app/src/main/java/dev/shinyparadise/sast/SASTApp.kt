package dev.shinyparadise.sast

import android.app.Application
import dev.shinyparadise.sast.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class SASTApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@SASTApp)
            modules(appModule)
        }
    }
}
