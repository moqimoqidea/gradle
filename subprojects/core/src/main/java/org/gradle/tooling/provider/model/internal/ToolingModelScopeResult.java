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

package org.gradle.tooling.provider.model.internal;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The outcome of building a model in a {@link ToolingModelScope}: the {@link #getClientResult() result returned to the
 * client} and, when resilient model building did not throw a failure straight away, the failure that must still fail
 * the build. At most one of {@link #getConfigurationFailure()} / {@link #getModelBuilderFailure()} is set. Keeping
 * these here rather than on the client result keeps the client result free of build-lifecycle concerns.
 */
@NullMarked
public final class ToolingModelScopeResult {

    @Nullable
    private final Throwable configurationFailure;
    @Nullable
    private final Throwable modelBuilderFailure;
    private final ToolingModelBuilderResultInternal clientResult;

    private ToolingModelScopeResult(@Nullable Throwable configurationFailure, @Nullable Throwable modelBuilderFailure, ToolingModelBuilderResultInternal clientResult) {
        this.configurationFailure = configurationFailure;
        this.modelBuilderFailure = modelBuilderFailure;
        this.clientResult = clientResult;
    }

    /**
     * The model built cleanly.
     */
    public static ToolingModelScopeResult of(ToolingModelBuilderResultInternal clientResult) {
        return new ToolingModelScopeResult(null, null, clientResult);
    }

    /**
     * A project or the build itself failed to configure.
     */
    public static ToolingModelScopeResult withConfigurationFailure(ToolingModelBuilderResultInternal clientResult, Throwable failure) {
        return new ToolingModelScopeResult(failure, null, clientResult);
    }

    /**
     * A tooling model builder threw after its target project had configured successfully.
     */
    public static ToolingModelScopeResult withModelBuilderFailure(ToolingModelBuilderResultInternal clientResult, Throwable failure) {
        return new ToolingModelScopeResult(null, failure, clientResult);
    }

    /**
     * A configuration failure hidden behind {@link #getClientResult() the result}, or {@code null} if none.
     */
    @Nullable
    public Throwable getConfigurationFailure() {
        return configurationFailure;
    }

    /**
     * A model builder failure hidden behind {@link #getClientResult() the result}, or {@code null} if none.
     */
    @Nullable
    public Throwable getModelBuilderFailure() {
        return modelBuilderFailure;
    }

    public ToolingModelBuilderResultInternal getClientResult() {
        return clientResult;
    }
}
