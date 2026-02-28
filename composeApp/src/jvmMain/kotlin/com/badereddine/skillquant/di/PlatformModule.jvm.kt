package com.badereddine.skillquant.di

import com.badereddine.skillquant.auth.GoogleAuthHelper
import org.koin.dsl.module

actual val platformModule = module {
    single { GoogleAuthHelper() }
}

