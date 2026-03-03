package com.badereddine.skillquant.android

import android.app.Application
import com.badereddine.skillquant.di.appModule
import com.badereddine.skillquant.di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class SkillQuantApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@SkillQuantApp)
            modules(appModule, platformModule)
        }
    }
}

