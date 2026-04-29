package dev.shinyparadise.sast.di

import dev.shinyparadise.sast.data.ApkDecompiler
import dev.shinyparadise.sast.data.SettingsRepository
import dev.shinyparadise.sast.domain.AICoreSmaliDiscoverer
import dev.shinyparadise.sast.domain.AnalyzerInteractor
import dev.shinyparadise.sast.domain.DeviceCapabilities
import dev.shinyparadise.sast.domain.HeuristicSmaliDiscoverer
import dev.shinyparadise.sast.domain.OnDeviceAIAnalyzer
import dev.shinyparadise.sast.domain.ReportGenerator
import dev.shinyparadise.sast.domain.ReportGeneratorImpl
import dev.shinyparadise.sast.domain.SmaliVulnerabilityDiscoverer
import dev.shinyparadise.sast.ui.screens.main.MainViewModel
import dev.shinyparadise.sast.ui.screens.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {

    viewModelOf(::MainViewModel)
    viewModelOf(::SettingsViewModel)

    single { SettingsRepository(androidContext()) }
    single { DeviceCapabilities(androidContext()) }

    single { ReportGeneratorImpl() } bind ReportGenerator::class

    single<ApkDecompiler> { ApkDecompiler(androidContext()) }

    single { OnDeviceAIAnalyzer() }

    single { HeuristicSmaliDiscoverer() }
    single<SmaliVulnerabilityDiscoverer> { AICoreSmaliDiscoverer(get(), get<HeuristicSmaliDiscoverer>()) }

    single { AnalyzerInteractor(androidContext(), get(), get(), get(), get(), get()) }
}
