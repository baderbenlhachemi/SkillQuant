package com.badereddine.skillquant.di

import com.badereddine.skillquant.auth.GoogleAuthHelper
import com.badereddine.skillquant.data.repository.FirestoreAlertRepository
import com.badereddine.skillquant.data.repository.FirestoreSkillRepository
import com.badereddine.skillquant.data.repository.FirestoreUserRepository
import com.badereddine.skillquant.domain.repository.AlertRepository
import com.badereddine.skillquant.domain.repository.SkillRepository
import com.badereddine.skillquant.domain.repository.UserRepository
import com.badereddine.skillquant.ui.calculator.SalaryCalculatorViewModel
import com.badereddine.skillquant.ui.comparison.ComparisonViewModel
import com.badereddine.skillquant.ui.dashboard.DashboardViewModel
import com.badereddine.skillquant.ui.detail.SkillDetailViewModel
import com.badereddine.skillquant.ui.learningpath.LearningPathViewModel
import com.badereddine.skillquant.ui.news.NewsViewModel
import com.badereddine.skillquant.ui.onboarding.OnboardingViewModel
import com.badereddine.skillquant.ui.radar.RadarViewModel
import com.badereddine.skillquant.ui.settings.AlertsSettingsViewModel
import com.badereddine.skillquant.util.ThemeManager
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
    // Firebase instances
    single { Firebase.firestore }
    single { Firebase.auth }

    // Singletons
    single { ThemeManager() }

    // Repositories
    singleOf(::FirestoreSkillRepository) bind SkillRepository::class
    singleOf(::FirestoreAlertRepository) bind AlertRepository::class
    singleOf(::FirestoreUserRepository) bind UserRepository::class

    // ViewModels
    factoryOf(::DashboardViewModel)
    factory { (skillId: String, location: String) -> SkillDetailViewModel(skillId, location, get(), get()) }
    factoryOf(::AlertsSettingsViewModel)
    factory { (location: String) -> ComparisonViewModel(get(), location) }
    factory { (location: String) -> SalaryCalculatorViewModel(get(), get(), location) }
    factory { (location: String) -> LearningPathViewModel(get(), get(), location) }
    factory { (location: String) -> RadarViewModel(get(), get(), location) }
    factoryOf(::NewsViewModel)
    factoryOf(::OnboardingViewModel)
}

/**
 * Platform-specific module that provides GoogleAuthHelper.
 * Android: uses Context from Koin's androidContext.
 * JVM/Desktop: provides a stub.
 */
expect val platformModule: org.koin.core.module.Module

