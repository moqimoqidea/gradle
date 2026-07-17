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

package org.gradle.internal.cc.impl.serialize

import org.gradle.internal.cc.base.exceptions.ConfigurationCacheThrowable
import org.gradle.internal.configuration.problems.StructuredMessage

/**
 * Signals that a configuration cache fingerprint entry could not be deserialized while checking whether the entry can
 * be reused. The [reason] describes the failure so the entry can be invalidated with a message that points at its cause.
 */
internal open class FingerprintDeserializationException(
    val reason: StructuredMessage,
    cause: Throwable
) : Exception(reason.render(), cause), ConfigurationCacheThrowable
