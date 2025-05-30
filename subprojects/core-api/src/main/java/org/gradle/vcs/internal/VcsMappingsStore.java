/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.vcs.internal;

import org.gradle.api.Action;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.invocation.Gradle;
import org.gradle.internal.service.scopes.Scope;
import org.gradle.internal.service.scopes.ServiceScope;
import org.gradle.vcs.VcsMapping;
import org.gradle.vcs.VersionControlSpec;
import org.jspecify.annotations.Nullable;

@ServiceScope(Scope.BuildTree.class)
public interface VcsMappingsStore {
    VcsResolver asResolver();

    void addRule(Action<? super VcsMapping> rule, Gradle gradle);

    VcsResolver NO_OP = new VcsResolver() {
        @Nullable
        @Override
        public VersionControlSpec locateVcsFor(ModuleComponentSelector selector) {
            return null;
        }

        @Override
        public boolean hasRules() {
            return false;
        }
    };
}
