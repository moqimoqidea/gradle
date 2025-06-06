/*
 * Copyright 2023 the original author or authors.
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

package org.gradle.internal.classpath.types;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.gradle.internal.UncheckedException;
import org.gradle.internal.hash.HashCode;
import org.gradle.internal.hash.StreamHasher;
import org.gradle.internal.lazy.Lazy;
import org.gradle.internal.service.scopes.Scope;
import org.gradle.internal.service.scopes.ServiceScope;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Provides instrumented super types for a given type for core Gradle types.
 *
 * Note: It returns only instrumented types that are instrumented with {@link org.gradle.internal.instrumentation.api.annotations.InterceptInherited}.
 */
@ServiceScope({Scope.Global.class, Scope.UserHome.class})
public class GradleCoreInstrumentationTypeRegistry implements InstrumentationTypeRegistry {

    /**
     * Generated by gradlebuild.distributions plugin
     */
    private static final String INSTRUMENTED_SUPER_TYPES_FILE = "/instrumented-super-types.properties";
    private static final String UPGRADED_PROPERTIES_FILE = "/upgraded-properties.json";

    private final StreamHasher streamHasher;
    private final Lazy<Map<String, Set<String>>> instrumentedSuperTypes;
    private final Lazy<Optional<HashCode>> instrumentedHashCode;
    private final Lazy<Optional<HashCode>> upgradedPropertiesHashCode;

    public GradleCoreInstrumentationTypeRegistry(StreamHasher streamHasher) {
        this.streamHasher = streamHasher;
        this.instrumentedSuperTypes = Lazy.locking().of(this::loadInstrumentedSuperTypes);
        this.instrumentedHashCode = Lazy.locking().of(this::loadInstrumentedSuperTypesHash);
        this.upgradedPropertiesHashCode = Lazy.locking().of(this::loadUpgradedPropertiesHash);
    }

    @Override
    public Set<String> getSuperTypes(String type) {
        return instrumentedSuperTypes.get().getOrDefault(type, Collections.emptySet());
    }

    @Override
    public boolean isEmpty() {
        return instrumentedSuperTypes.get().isEmpty();
    }

    public Optional<HashCode> getInstrumentedTypesHash() {
        return instrumentedHashCode.get();
    }

    public Optional<HashCode> getUpgradedPropertiesHash() {
        return upgradedPropertiesHashCode.get();
    }

    @SuppressWarnings("MethodMayBeStatic")
    private Map<String, Set<String>> loadInstrumentedSuperTypes() {
        try (InputStream stream = GradleCoreInstrumentationTypeRegistry.class.getResourceAsStream(INSTRUMENTED_SUPER_TYPES_FILE)) {
            if (stream == null) {
                return Collections.emptyMap();
            }
            Properties properties = new Properties();
            properties.load(stream);
            ImmutableMap.Builder<String, Set<String>> builder = ImmutableMap.builder();
            properties.forEach((k, v) -> builder.put((String) k, ImmutableSet.copyOf(v.toString().split(","))));
            return builder.build();
        } catch (IOException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        }
    }

    private Optional<HashCode> loadInstrumentedSuperTypesHash() {
        return loadHashCodeFromResource(INSTRUMENTED_SUPER_TYPES_FILE);
    }

    private Optional<HashCode> loadUpgradedPropertiesHash() {
        return loadHashCodeFromResource(UPGRADED_PROPERTIES_FILE);
    }

    private Optional<HashCode> loadHashCodeFromResource(String resourceFile) {
        try (InputStream stream = GradleCoreInstrumentationTypeRegistry.class.getResourceAsStream(resourceFile)) {
            if (stream == null) {
                return Optional.empty();
            }
            return Optional.of(streamHasher.hash(stream));
        } catch (IOException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        }
    }
}
