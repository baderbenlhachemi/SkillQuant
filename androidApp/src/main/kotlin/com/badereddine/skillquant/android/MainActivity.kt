package com.badereddine.skillquant.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.badereddine.skillquant.App
import com.badereddine.skillquant.auth.GoogleAuthHelper
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    // Inject GoogleAuthHelper so we can wire the Activity reference into it.
    // Koin is already started by SkillQuantApp before this runs.
    private val googleAuthHelper: GoogleAuthHelper by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Wire Activity context — CredentialManager requires an Activity on real devices
        googleAuthHelper.activityContext = this

        setContent {
            App()
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-wire after returning from background / other activities
        googleAuthHelper.activityContext = this
    }

    override fun onDestroy() {
        super.onDestroy()
        // Avoid leaking the Activity reference
        googleAuthHelper.activityContext = null
    }
}
