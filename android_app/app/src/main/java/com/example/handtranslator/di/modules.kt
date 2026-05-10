package com.example.handtranslator.di

import com.example.handtranslator.translator.TranslationPanel
import com.example.handtranslator.translator.TranslatorViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val translateModule = module {
    viewModel { TranslatorViewModel(androidApplication()) }
}