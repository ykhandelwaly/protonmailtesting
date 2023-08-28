/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
import com.android.build.gradle.TestedExtension
import org.gradle.api.JavaVersion
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import studio.forface.easygradle.dsl.*
import studio.forface.easygradle.dsl.android.*

/**
 * Dsl for apply the android configuration to a library or application module
 * @author Davide Farella
 */
@Suppress("LongMethod") // This is a setup for Android module, it does not contain any logic and doesn't
//                                   make sense to split it
fun org.gradle.api.Project.android(

    appIdSuffix: String? = null,
    compileSdk: Int = ProtonMail.compileSdk,
    minSdk: Int = ProtonMail.minSdk,
    targetSdk: Int = ProtonMail.targetSdk,
    version: Version? = null,
    versionCode: Int = ProtonMail.versionCode,
    versionName: String = ProtonMail.versionName,
    config: ExtraConfig = {}

) = (this as ExtensionAware).extensions.configure<TestedExtension> {

    compileSdkVersion(compileSdk)
    ndkVersion = "21.3.6528147" // Same as Docker image
    defaultConfig {

        // Params
        appIdSuffix?.let { applicationId = "ch.protonmail.$it" }
        if (version != null) {
            this.version = version
        } else {
            this.versionCode = versionCode
            this.versionName = versionName
        }

        // SDK
        minSdkVersion(minSdk)
        targetSdkVersion(targetSdk)

        // Other
        vectorDrawables.useSupportLibrary = true
        buildFeatures.viewBinding = true
    }

    lintOptions {
        disable("InvalidPackage")
        disable("MissingTranslation")
        disable("ExtraTranslation")
    }
    // Add support for `src/x/kotlin` instead of `src/x/java` only
    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
        getByName("test").java.srcDirs("src/test/kotlin")
        getByName("androidTest").java.srcDirs("src/androidTest/kotlin", "src/uiTest/kotlin")
        getByName("androidTest").assets.srcDirs("src/androidTest/assets", "src/uiTest/assets")
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = ProtonMail.jvmTarget
        targetCompatibility = sourceCompatibility
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        animationsDisabled = true
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    buildFeatures.viewBinding = true

    packagingOptions {
        exclude("MANIFEST.MF")
        exclude("META-INF/*.kotlin_module")
        exclude("META-INF/AL2.0")
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/DEPENDENCIES.txt")
        exclude("META-INF/LGPL2.1")
        exclude("META-INF/LICENSE")
        exclude("META-INF/LICENSE.md")
        exclude("META-INF/LICENSE.txt")
        exclude("META-INF/LICENSE-notice.md")
        exclude("META-INF/NOTICE")
        exclude("META-INF/NOTICE.txt")
        exclude("META-INF/rxjava.properties")
    }

    apply(config)
}

typealias ExtraConfig = TestedExtension.() -> Unit
