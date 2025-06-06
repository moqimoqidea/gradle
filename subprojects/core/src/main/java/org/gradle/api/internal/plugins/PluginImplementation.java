/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.api.internal.plugins;

import org.gradle.internal.DisplayName;
import org.gradle.plugin.use.PluginId;
import org.jspecify.annotations.Nullable;

public interface PluginImplementation<T> extends PotentialPlugin<T> {
    DisplayName getDisplayName();

    /**
     * An id for the plugin implementation, if known.
     */
    @Nullable
    PluginId getPluginId();

    boolean isAlsoKnownAs(PluginId id);
}
