import java.util.Properties
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
}

// ── Load secrets from local.properties ──────────────────────────────────────
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}
val firebaseApiKey: String = localProps.getProperty("FIREBASE_API_KEY")
    ?: System.getenv("FIREBASE_API_KEY")
    ?: error("FIREBASE_API_KEY not found in local.properties or environment")

// ── Configuration-cache-compatible task types ────────────────────────────────
abstract class InjectApiKeyTask : DefaultTask() {
    @get:Input abstract val apiKey: Property<String>
    @get:org.gradle.api.tasks.Internal
    abstract val googleServicesFile: RegularFileProperty

    @TaskAction
    fun inject() {
        val f = googleServicesFile.get().asFile
        val content = f.readText()
        if (content.contains("PLACEHOLDER-API-KEY")) {
            f.writeText(content.replace("PLACEHOLDER-API-KEY", apiKey.get()))
            println("✅ Injected FIREBASE_API_KEY into google-services.json")
        }
    }
}

abstract class RestoreApiKeyTask : DefaultTask() {
    @get:Input abstract val apiKey: Property<String>
    @get:org.gradle.api.tasks.Internal
    abstract val googleServicesFile: RegularFileProperty

    @TaskAction
    fun restore() {
        val f = googleServicesFile.get().asFile
        val content = f.readText()
        if (content.contains(apiKey.get())) {
            f.writeText(content.replace(apiKey.get(), "PLACEHOLDER-API-KEY"))
            println("✅ Restored placeholder in google-services.json")
        }
    }
}

// ── Register tasks ────────────────────────────────────────────────────────────
val googleServicesJsonFile = layout.projectDirectory.file("google-services.json")

val injectApiKey by tasks.registering(InjectApiKeyTask::class) {
    apiKey.set(firebaseApiKey)
    googleServicesFile.set(googleServicesJsonFile)
}

val restoreApiKeyPlaceholder by tasks.registering(RestoreApiKeyTask::class) {
    apiKey.set(firebaseApiKey)
    googleServicesFile.set(googleServicesJsonFile)
}

tasks.whenTaskAdded {
    if (name.startsWith("process") && name.endsWith("GoogleServices")) {
        dependsOn(injectApiKey)
        finalizedBy(restoreApiKeyPlaceholder)
    }
}

android {
    namespace = "com.badereddine.skillquant.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.badereddine.skillquant"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.koin.android)
    debugImplementation(libs.compose.uiTooling)
}
