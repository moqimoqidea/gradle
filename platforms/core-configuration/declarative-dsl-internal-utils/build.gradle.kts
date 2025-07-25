/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    id("gradlebuild.distribution.implementation-kotlin")
}

description = "DCL-related utilities that are meant to be reused in the Gradle subprojects but not in DCL client applications"

dependencies {
    api(projects.core)

    implementation(projects.declarativeDslApi)
    implementation(projects.coreApi)

    api(libs.futureKotlin("stdlib"))
    implementation(libs.futureKotlin("reflect"))

    testImplementation(projects.internalTesting)
}
