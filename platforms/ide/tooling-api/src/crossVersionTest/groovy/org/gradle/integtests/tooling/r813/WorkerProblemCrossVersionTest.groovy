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

package org.gradle.integtests.tooling.r813

import org.gradle.integtests.tooling.fixture.TargetGradleVersion
import org.gradle.integtests.tooling.fixture.ToolingApiSpecification
import org.gradle.integtests.tooling.fixture.ToolingApiVersion
import org.gradle.tooling.events.ProgressEvent
import org.gradle.tooling.events.ProgressListener
import org.gradle.tooling.events.problems.SingleProblemEvent
import org.gradle.workers.fixtures.WorkerExecutorFixture

@ToolingApiVersion('>=8.13')
@TargetGradleVersion('>=8.13')
class WorkerProblemCrossVersionTest extends ToolingApiSpecification {

    def setup() {
        settingsFile << """
            rootProject.name = 'root'
        """
        buildFile << """
            plugins {
                id 'java-library'
            }
        """
    }

    def "problem from worker using #isolationMode"() {
        setup:
        file('buildSrc/build.gradle') << """
            plugins {
                id 'java'
            }

            dependencies {
                implementation(gradleApi())
            }
        """
        file('buildSrc/src/main/java/org/gradle/test/ProblemsWorkerTaskParameter.java') << """
            package org.gradle.test;

            import org.gradle.workers.WorkParameters;

            public interface ProblemsWorkerTaskParameter extends WorkParameters { }
        """
        file('buildSrc/src/main/java/org/gradle/test/ProblemWorkerTask.java') << """
            package org.gradle.test;

            import java.io.File;
            import java.io.FileWriter;
            import org.gradle.api.problems.Problems;
            import org.gradle.api.problems.ProblemId;
            import org.gradle.api.problems.ProblemGroup;
            import org.gradle.internal.operations.CurrentBuildOperationRef;

            import org.gradle.workers.WorkAction;

            import javax.inject.Inject;

            public abstract class ProblemWorkerTask implements WorkAction<ProblemsWorkerTaskParameter> {

                @Inject
                public abstract Problems getProblems();

                @Override
                public void execute() {
                    ProblemId problemId = ProblemId.create("name", "Display name", ProblemGroup.create("generic", "Generic"));
                    getProblems().getReporter().report(problemId, problem ->
                        problem.contextualLabel("Tooling API client should receive this problem")
                    );
                }
            }
        """
        buildFile << """
            import javax.inject.Inject
            import org.gradle.test.ProblemWorkerTask


            abstract class ProblemTask extends DefaultTask {
                @Inject
                abstract WorkerExecutor getWorkerExecutor();

                @TaskAction
                void executeTask() {
                    getWorkerExecutor().${isolationMode}().submit(ProblemWorkerTask.class) {}
                }
            }

            tasks.register("reportProblem", ProblemTask)
        """
        def problemProgressListener = new ProblemProgressListener()

        when:

        withConnection { connection ->
            connection.newBuild()
                .forTasks('reportProblem')
                .addProgressListener(problemProgressListener)
                .run()
        }

        then:
        def event = problemProgressListener.problemEvents.find {it.problem.definition.id.name == 'name' }
        event.problem.definition.id.displayName == 'Display name'
        event.problem.contextualLabel.contextualLabel == 'Tooling API client should receive this problem'

        where:
        isolationMode << WorkerExecutorFixture.IsolationMode.values().collect {it.method }
    }


    class ProblemProgressListener implements ProgressListener {

        List<SingleProblemEvent> problemEvents = []

        @Override
        void statusChanged(ProgressEvent event) {
            if (event instanceof SingleProblemEvent) {
                problemEvents += event
            }
        }
    }
}
