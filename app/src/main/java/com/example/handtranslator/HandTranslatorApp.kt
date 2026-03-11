package com.example.handtranslator

import android.app.Application
import com.example.handtranslator.di.translateModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class HandTranslatorApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            modules(
                translateModule
            )
            androidContext(applicationContext)
        }
    }
}