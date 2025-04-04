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

package org.gradle.language.nativeplatform.internal.incremental;

import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.TaskInternal;
import org.gradle.api.provider.Provider;
import org.gradle.internal.service.scopes.Scope;
import org.gradle.internal.service.scopes.ServiceScope;
import org.gradle.language.base.internal.compile.Compiler;
import org.gradle.nativeplatform.toolchain.internal.NativeCompileSpec;

import java.util.Map;

@ServiceScope({Scope.Build.class, Scope.Project.class})
public interface IncrementalCompilerBuilder {
    IncrementalCompiler newCompiler(TaskInternal task, FileCollection sourceFiles, FileCollection includeDirs, Map<String, String> macros, Provider<Boolean> importAware);

    interface IncrementalCompiler {
        <T extends NativeCompileSpec> Compiler<T> createCompiler(Compiler<T> compiler);

        /**
         * Returns a file collection that contains the header files required by the source files.
         */
        FileCollection getHeaderFiles();
    }
}
