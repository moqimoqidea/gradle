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

package org.gradle.api.problems.internal;

import org.gradle.api.problems.Problem;
import org.gradle.internal.service.scopes.Scope;
import org.gradle.internal.service.scopes.ServiceScope;

import java.io.File;
import java.util.List;

@ServiceScope(Scope.BuildTree.class)
public interface ProblemReportCreator {
    /**
     * Stores a new problem in a temporary file that will be added to the final report when #createProblem is called.
     */
    void addProblem(Problem problem);

    /**
     * Renders a new problem report in the target directory. The report will contain the summaries of the omitted problem reports. The report file is only created if there's at least one problem reported.
     */
    void createReportFile(File reportDir, List<ProblemSummaryData> problemSummaries);
}
