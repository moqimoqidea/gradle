/*
 * Copyright 2021 the original author or authors.
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

package org.gradle.smoketests

import org.gradle.integtests.fixtures.executer.IntegrationTestBuildContext
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.model.GradleProject
import org.gradle.tooling.model.build.BuildEnvironment
import org.gradle.tooling.model.eclipse.EclipseProject
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject
import org.gradle.tooling.model.gradle.BuildInvocations
import org.gradle.tooling.model.gradle.GradleBuild
import org.gradle.tooling.model.gradle.ProjectPublications
import org.gradle.tooling.model.idea.BasicIdeaProject
import org.gradle.tooling.model.idea.IdeaProject

class GradleBuildToolingApiSmokeTest extends AbstractGradleceptionSmokeTest {
    def "can load model #model when parallel=#parallel (TAPI current -> Gradle current)"() {
        when:
        // Use the smoke test args by creating the runner
        def runner = runner()
        def modelResult
        try (ProjectConnection connector = GradleConnector.newConnector()
            .useGradleUserHomeDir(IntegrationTestBuildContext.INSTANCE.gradleUserHomeDir)
            .useInstallation(IntegrationTestBuildContext.INSTANCE.gradleHomeDir)
            .forProjectDirectory(testProjectDir)
            .connect()) {
            modelResult = connector.model(model)
                .addArguments(runner.arguments)
                .addArguments(parallel ? '--parallel' : '--no-parallel')
                .addJvmArguments(runner.jvmArguments)
                .setStandardOutput(System.out)
                .setStandardError(System.err)
                .get()
        }

        then:
        modelResult != null

        where:
        [parallel, model] << [
            [true, false],
            [
                BasicIdeaProject,
                BuildEnvironment,
                BuildInvocations,
                EclipseProject,
                GradleBuild,
                GradleProject,
                HierarchicalEclipseProject,
                IdeaProject,
                ProjectPublications
            ]
        ].combinations()
    }
}
