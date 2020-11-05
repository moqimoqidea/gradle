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

package org.gradle.kotlin.dsl.cache

import org.gradle.api.internal.cache.StringInterner
import org.gradle.cache.CacheRepository
import org.gradle.cache.internal.CacheScopeMapping
import org.gradle.cache.internal.InMemoryCacheDecoratorFactory
import org.gradle.cache.internal.VersionStrategy
import org.gradle.internal.execution.workspace.WorkspaceProvider
import org.gradle.internal.execution.workspace.impl.DefaultImmutableWorkspaceProvider
import org.gradle.internal.file.FileAccessTimeJournal
import org.gradle.kotlin.dsl.accessors.accessorsWorkspacePrefix
import java.io.Closeable


class KotlinDslWorkspaceProvider(
    cacheRepository: CacheRepository,
    cacheScopeMapping: CacheScopeMapping,
    fileAccessTimeJournal: FileAccessTimeJournal,
    inMemoryCacheDecoratorFactory: InMemoryCacheDecoratorFactory,
    stringInterner: StringInterner
) : WorkspaceProvider, Closeable {
    private
    val delegate = DefaultImmutableWorkspaceProvider.withBuiltInHistory(
        cacheRepository
            .cache(cacheScopeMapping.getBaseDirectory(null, "kotlin-dsl", VersionStrategy.CachePerVersion))
            .withDisplayName("kotlin-dsl"),
        fileAccessTimeJournal,
        inMemoryCacheDecoratorFactory,
        stringInterner
    )

    override fun <T : Any> withWorkspace(path: String, action: WorkspaceProvider.WorkspaceAction<T>): T =
        delegate.withWorkspace("$accessorsWorkspacePrefix/$path", action)

    override fun close() = delegate.close()
}
