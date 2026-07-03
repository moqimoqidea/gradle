/*
 * Copyright 2026 the original author or authors.
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

package org.gradle.integtests.tooling.r970

import org.gradle.integtests.tooling.fixture.TargetGradleVersion
import org.gradle.integtests.tooling.fixture.ToolingApiVersion
import org.gradle.integtests.tooling.r16.CustomModel
import org.gradle.integtests.tooling.r930.KotlinDslPluginRelatedToolingApiSpecification
import org.gradle.tooling.BuildException
import org.gradle.tooling.IntermediateResultHandler

@ToolingApiVersion('>=9.7.0')
@TargetGradleVersion('>=9.7.0')
class ResilientPerProjectFailureCrossVersionSpec extends KotlinDslPluginRelatedToolingApiSpecification {

    private static final List<String> IP = ["-Dorg.gradle.isolated-projects=true"]

    // The exact client-facing failure for a project with no failure of its own (clean or never-reached). Asserted in
    // full so this text is a guarantee: changing it must be a deliberate change that updates this test.
    private static final String GENERAL_CONFIGURATION_FAILURE = "The build could not be configured; see the reported build failures for the underlying problems."

    private FetchFailureTreeAction.Result fetchResult

    def setup() {
        settingsFile.delete()
        file('init.gradle') << """
            import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
            import org.gradle.tooling.provider.model.ToolingModelBuilder
            import javax.inject.Inject

            gradle.lifecycle.beforeProject {
                it.plugins.apply(CustomPlugin)
            }

            class CustomModel implements Serializable {
                String getValue() { 'greetings' }
            }
            class CustomBuilder implements ToolingModelBuilder {
                boolean canBuild(String modelName) { modelName == '${CustomModel.name}' }
                Object buildAll(String modelName, Project project) { new CustomModel() }
            }
            class CustomPlugin implements Plugin<Project> {
                @Inject CustomPlugin(ToolingModelBuilderRegistry registry) { registry.register(new CustomBuilder()) }
                void apply(Project project) {}
            }
        """.stripIndent()
        settingsKotlinFile << """
            rootProject.name = "root"
            include("a", "b", "c")
        """.stripIndent()
        file("a/build.gradle.kts") << "// intentionally clean\n"
        file("b/build.gradle.kts") << 'throw RuntimeException("FAILURE(:b)")\n'
        file("c/build.gradle.kts") << 'throw RuntimeException("FAILURE(:c)")\n'
    }

    def "each failing project reports only its own failure and clean projects report neither, while the build still fails"() {
        when:
        fetchFailures(IP)

        then: "behaviour is unchanged: the whole build fails to configure, so every project fails to be queried and the build fails"
        thrown(BuildException)
        def result = fetchResult
        result.successfullyQueriedProjects == []
        result.failedToQueryProjects.toSet() == ["root", "a", "b", "c"] as Set

        and: "each failing project's client failure carries only its own marker, not the sibling's whole-build aggregate"
        treeContains(result, "b", "FAILURE(:b)")
        !treeContains(result, "b", "FAILURE(:c)")
        treeContains(result, "c", "FAILURE(:c)")
        !treeContains(result, "c", "FAILURE(:b)")

        and: "clean projects carry the general message, not either sibling's failure or the whole-build aggregate"
        treeContains(result, "root", GENERAL_CONFIGURATION_FAILURE)
        treeContains(result, "a", GENERAL_CONFIGURATION_FAILURE)
        !treeContains(result, "root", "FAILURE(:b)")
        !treeContains(result, "root", "FAILURE(:c)")
        !treeContains(result, "a", "FAILURE(:b)")
        !treeContains(result, "a", "FAILURE(:c)")
    }

    private void fetchFailures(List<String> extraGradleProperties = []) {
        fails {
            action()
                .buildFinished(new FetchFailureTreeAction(CustomModel), { fetchResult = it } as IntermediateResultHandler)
                .build()
                .withArguments("--init-script=${file('init.gradle').absolutePath}", *extraGradleProperties)
                .run()
        }
    }

    private static boolean treeContains(FetchFailureTreeAction.Result result, String project, String marker) {
        return nodeContains(result.failureTreeByProject[project], marker)
    }

    private static boolean nodeContains(FetchFailureTreeAction.FailureNode node, String marker) {
        if (node == null) {
            return false
        }
        if ((node.message ?: "").contains(marker)) {
            return true
        }
        return node.causes.any { nodeContains(it, marker) }
    }
}
