package dev.shinyparadise.sast.di

import dev.shinyparadise.sast.data.ApkDecompiler
import dev.shinyparadise.sast.domain.AnalyzerInteractor
import dev.shinyparadise.sast.ui.screens.main.MainViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {

    viewModelOf(::MainViewModel)

    singleOf(::ApkDecompiler)
    singleOf(::AnalyzerInteractor)
}
