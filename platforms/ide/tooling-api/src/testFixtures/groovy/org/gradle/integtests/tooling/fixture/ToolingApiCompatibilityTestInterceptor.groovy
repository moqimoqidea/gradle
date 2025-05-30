/*
 * Copyright 2020 the original author or authors.
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

package org.gradle.integtests.tooling.fixture

import org.gradle.integtests.fixtures.GradleDistributionTool
import org.gradle.integtests.fixtures.compatibility.AbstractCompatibilityTestInterceptor
import org.gradle.integtests.fixtures.compatibility.CoverageContext

class ToolingApiCompatibilityTestInterceptor extends AbstractCompatibilityTestInterceptor {

    protected ToolingApiCompatibilityTestInterceptor(Class<?> target) {
        super(target)
    }

    @Override
    protected void createExecutionsForContext(CoverageContext coverageContext) {
        if (coverageContext == CoverageContext.DEFAULT) {
            add(new ToolingApiExecution(current, current))
        } else {
            super.createExecutionsForContext(coverageContext)
        }
    }

    @Override
    protected Collection<Execution> createExecutionsFor(GradleDistributionTool versionedTool) {
        return [new ToolingApiExecution(current, versionedTool.distribution)]
    }

}
