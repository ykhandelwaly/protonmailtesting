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
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.ScriptHandlerScope
import studio.forface.easygradle.dsl.*
import studio.forface.easygradle.dsl.android.*

/**
 * Lambda that applies dependencies to the classpath
 * @author Davide Farella
 */
val ScriptHandlerScope.classpathDependencies: DependencyHandlerScope.() -> Unit get() = {
    classpath(`android-gradle-plugin`)
    classpath(`browserstack-gradle-plugin`)
    classpath(`google-services`)
    classpath(`hilt-android-gradle-plugin`)
    classpath(`kotlin-gradle-plugin`)
    classpath(`serialization-gradle-plugin`)
    classpath(`sentry-android-plugin`)
}
