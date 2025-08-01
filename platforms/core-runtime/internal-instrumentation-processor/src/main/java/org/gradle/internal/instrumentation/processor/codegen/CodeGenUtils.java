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

package org.gradle.internal.instrumentation.processor.codegen;

import com.squareup.javapoet.AnnotationSpec;

public class CodeGenUtils {
    public static final AnnotationSpec SUPPRESS_UNCHECKED_AND_RAWTYPES = AnnotationSpec.builder(SuppressWarnings.class)
        .addMember("value", "$L", "{\"unchecked\", \"rawtypes\"}")
        .build();

    public static final AnnotationSpec SUPPRESS_DEPRECATIONS = AnnotationSpec.builder(SuppressWarnings.class)
        .addMember("value", "$S", "deprecation")
        .build();
}
