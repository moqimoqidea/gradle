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

package org.gradle.kotlin.dsl.compile

import org.gradle.integtests.fixtures.UnsupportedWithConfigurationCache
import org.gradle.kotlin.dsl.provider.KOTLIN_SCRIPT_COMPILATION_AVOIDANCE_ENABLED_PROPERTY
import org.junit.Assert.assertTrue
import org.junit.Test


class BuildScriptCompileAvoidanceIntegrationTest : AbstractCompileAvoidanceIntegrationTest() {

    @Test
    fun `script compilation avoidance can be disabled via a system property`() {

        withFile("gradle.properties", "systemProp.$KOTLIN_SCRIPT_COMPILATION_AVOIDANCE_ENABLED_PROPERTY=false")

        val className = givenJavaClassInBuildSrcContains("""public void foo() { System.out.println("foo"); }""")
        withUniqueScript("$className().foo()")
        configureProject().assertBuildScriptCompiled().assertOutputContains("foo")

        givenJavaClassInBuildSrcContains("""public void foo() { System.out.println("bar"); }""")
        configureProject().assertBuildScriptBodyRecompiled().assertOutputContains("bar")
    }

    @Test
    @UnsupportedWithConfigurationCache(because = "test rely on configuration phase output")
    fun `avoids buildscript recompilation on included build JAR rebuild`() {

        withDefaultSettingsIn("build-logic")
            .appendText("""rootProject.name = "build-logic"""")
        withKotlinDslPluginIn("build-logic")
        withFile("build-logic/src/main/kotlin/my-plugin.gradle.kts", "")
        val className = kotlinClassSourceFile("build-logic", """
            inline fun foo() { println("bar") }
        """)
        withSettings(""" pluginManagement { includeBuild("build-logic") } """)

        withUniqueScript("""
            plugins { id("my-plugin") }
            $className().foo()
        """)
        configureProject().assertBuildScriptCompiled().assertOutputContains("bar")

        // Delete the JAR as this is not cacheable and by default JARs are not reproducible
        require(existing("build-logic/build/libs/build-logic.jar").delete())

        configureProject().assertBuildScriptCompilationAvoided().assertOutputContains("bar")
    }

    @Test
    fun `avoids buildscript recompilation on non ABI change in buildSrc`() {
        val className = givenJavaClassInBuildSrcContains(
            """
            public void foo() {
                System.out.println("foo");
            }
            """
        )
        withUniqueScript("$className().foo()")
        configureProject().assertBuildScriptCompiled().assertOutputContains("foo")

        givenJavaClassInBuildSrcContains(
            """
            public void foo() {
                System.out.println("bar");
            }
            """
        )
        configureProject().assertBuildScriptCompilationAvoided().assertOutputContains("bar")
    }

    @Test
    fun `avoids buildscript recompilation on resource file change in buildSrc`() {
        val className = givenJavaClassInBuildSrcContains(
            """
            public void foo() {
                System.out.println("foo");
            }
            """
        )
        withFile("buildSrc/src/main/resources/foo.txt", "foo")
        withUniqueScript("$className().foo()")
        configureProject().assertBuildScriptCompiled().assertOutputContains("foo")

        withFile("buildSrc/src/main/resources/foo.txt", "bar")
        configureProject().assertBuildScriptCompilationAvoided().assertOutputContains("foo")
    }

    @Test
    fun `avoids buildscript recompilation on non-code change in buildSrc`() {
        val className = givenJavaClassInBuildSrcContains(
            """
            public void foo() {
                System.out.println("foo");
            }
            """
        )
        withUniqueScript("$className().foo()")
        configureProject().assertBuildScriptCompiled().assertOutputContains("foo")

        givenJavaClassInBuildSrcContains(
            """
            public void foo() {
                // a comment
                System.out.println("foo");
            }
            """
        )
        configureProject().assertBuildScriptCompilationAvoided().assertOutputContains("foo")
    }

    @Test
    fun `recompiles buildscript on ABI change in buildSrc`() {
        val className = givenJavaClassInBuildSrcContains(
            """
            public void foo() {
                System.out.println("foo");
            }
            """
        )
        withUniqueScript("$className().foo()")
        configureProject().assertBuildScriptCompiled().assertOutputContains("foo")

        givenJavaClassInBuildSrcContains(
            """
            public void foo() {
                System.out.println("bar");
            }
            public void bar() {}
            """
        )
        configureProject().assertBuildScriptBodyRecompiled().assertOutputContains("bar")
    }

    @Test
    @UnsupportedWithConfigurationCache(because = "test rely on configuration phase output")
    fun `avoids buildscript recompilation on non ABI change in buildscript classpath`() {
        val (className, jarPath) = buildJarForBuildScriptClasspath(
            """
            public void foo() {
                System.out.println("foo");
            }
            """
        )

        withUniqueScript(
            """
            buildscript {
                dependencies { classpath(files("$jarPath")) }
            }
            $className().foo()
            """
        )
        configureProject().assertBuildScriptCompiled().assertOutputContains("foo")

        buildJarForBuildScriptClasspath(
            """
            public void foo() {
                System.out.println("bar");
            }
            """
        )
        configureProject().assertBuildScriptCompilationAvoided().assertOutputContains("bar")
    }

    @Test
    @UnsupportedWithConfigurationCache(because = "test rely on configuration phase output")
    fun `recompiles buildscript on ABI change in buildscript classpath`() {
        val (className, jarPath) = buildJarForBuildScriptClasspath(
            """
            public void foo() {
                System.out.println("foo");
            }
            """
        )

        withUniqueScript(
            """
            buildscript {
                dependencies { classpath(files("$jarPath")) }
            }
            $className().foo()
            """
        )
        configureProject().assertBuildScriptCompiled().assertOutputContains("foo")

        buildJarForBuildScriptClasspath(
            """
            public void foo() {
                System.out.println("bar");
            }
            public void bar() {}
            """
        )
        configureProject().assertBuildScriptBodyRecompiled().assertOutputContains("bar")
    }

    @Test
    @UnsupportedWithConfigurationCache(because = "test rely on configuration phase output")
    fun `avoids buildscript recompilation when jar that can not be used for compile avoidance initially on buildsript classpath is touched`() {
        val (className, jarPath) = buildKotlinJarForBuildScriptClasspath(
            """
            inline fun foo() {
                val sum: (Int, Int) -> Int = { x, y -> x + y }
                println("foo = " + sum(2, 2))
            }
            """
        )

        withUniqueScript(
            """
            buildscript {
                dependencies { classpath(files("$jarPath")) }
            }
            $className().foo()
            """
        )
        configureProject().assertBuildScriptCompiled().assertOutputContains("foo = 4")

        existing(jarPath).setLastModified(1)
        configureProject().assertBuildScriptCompilationAvoided().assertOutputContains("foo = 4")
    }

    @Test
    fun `recompiles buildscript on inline function change in buildSrc class`() {
        val className = givenKotlinClassInBuildSrcContains(
            """
            inline fun foo() {
                println("foo")
            }
            """
        )
        withUniqueScript("$className().foo()")
        configureProject().assertBuildScriptCompiled().assertOutputContains("foo")

        givenKotlinClassInBuildSrcContains(
            """
            inline fun foo() {
                println("bar")
            }
            """
        )
        configureProject().assertBuildScriptBodyRecompiled().assertOutputContains("bar")
    }

    @Test
    fun `recompiles buildscript on inline lambda function change in buildSrc class`() {
        val className = givenKotlinClassInBuildSrcContains(
            """
            inline fun foo() {
                val aggregate: (Int, Int) -> Int = { x, y -> x + y }
                println("foo = " + aggregate(2, 2))
            }
            """
        )
        withUniqueScript("$className().foo()")
        configureProject().assertBuildScriptCompiled().assertOutputContains("foo = 4")

        givenKotlinClassInBuildSrcContains(
            """
            inline fun foo() {
                val aggregate: (Int, Int) -> Int = { x, y -> x - y }
                println("foo = " + aggregate(2, 2))
            }
            """
        )
        configureProject().assertBuildScriptCompiled().assertOutputContains("foo = 0")
    }

    @Test
    @UnsupportedWithConfigurationCache(because = "test rely on configuration phase output")
    fun `avoids buildscript recompilation when resource file metadata is changed`() {
        val className = givenKotlinClassInBuildSrcContains(
            """
            inline fun foo() {
                val sum: (Int, Int) -> Int = { x, y -> x + y }
                println("foo = " + sum(2, 2))
            }
            """
        )
        withUniqueScript("$className().foo()")
        val resourceFile = withFile("buildSrc/src/main/resources/foo.txt", "foo")
        configureProject().assertBuildScriptCompiled().assertOutputContains("foo")

        resourceFile.setLastModified(1)
        resourceFile.setReadOnly()
        configureProject().assertBuildScriptCompilationAvoided().assertOutputContains("foo")
    }

    @Test
    fun `avoids buildscript recompilation on internal inline function change in buildSrc class`() {
        // discussed in: https://youtrack.jetbrains.com/issue/KT-62557/Wrong-ABI-fingerprint-for-public-function-delegating-to-internal-inline-function

        val className = givenKotlinClassInBuildSrcContains(
            """
            fun foo() = bar()
            internal inline fun bar() {
                println("foo")
            }
            """
        )
        withUniqueScript("$className().foo()")
        configureProject().assertBuildScriptCompiled().assertOutputContains("foo")

        givenKotlinClassInBuildSrcContains(
            """
            fun foo() = bar()
            internal inline fun bar() {
                println("bar")
            }
            """
        )
        configureProject().assertBuildScriptCompiled().assertOutputContains("bar")
    }

    @Test
    fun `recompiles buildscript on internal function visibility change to public in buildSrc class`() {
        val className = givenKotlinClassInBuildSrcContains(
            """
            fun foo() = bar()
            internal fun bar() {
                // do nothing
            }
            """
        )
        withUniqueScript("$className().foo()")
        configureProject().assertBuildScriptCompiled()

        givenKotlinClassInBuildSrcContains(
            """
            fun foo() = bar()
            fun bar() {
                // do nothing
            }
            """
        )
        configureProject().assertBuildScriptCompiled()
    }

    @Test
    fun `avoids buildscript recompilation on non ABI changes to multifile class in buildSrc`() {
        val multifileAnnotations = """
            @file:JvmName("Utils")
            @file:JvmMultifileClass
        """
        val packageName = givenKotlinScriptInBuildSrcContains(
            "foo",
            """
            fun foo() = "foo"
            """,
            multifileAnnotations
        )
        givenKotlinScriptInBuildSrcContains(
            "bar",
            """
            fun bar() = "bar"
            """,
            multifileAnnotations
        )
        withUniqueScript("println($packageName.foo() + $packageName.bar())")
        configureProject().assertBuildScriptCompiled().assertOutputContains("foobar")

        givenKotlinScriptInBuildSrcContains(
            "foo",
            """
            fun foo() = "bar"
            """,
            multifileAnnotations
        )
        givenKotlinScriptInBuildSrcContains(
            "bar",
            """
            fun bar() = "foo"
            """,
            multifileAnnotations
        )
        configureProject().assertBuildScriptCompilationAvoided().assertOutputContains("barfoo")
    }

    @Test
    fun `recompiles buildscript when inline function changes in multifile class in buildSrc`() {
        val multifileAnnotations = """
            @file:JvmName("Utils")
            @file:JvmMultifileClass
        """
        val packageName = givenKotlinScriptInBuildSrcContains(
            "foo",
            """
            inline fun foo() = "foo"
            """,
            multifileAnnotations
        )
        givenKotlinScriptInBuildSrcContains(
            "bar",
            """
            inline fun bar() = "bar"
            """,
            multifileAnnotations
        )
        withUniqueScript("println($packageName.foo() + $packageName.bar())")
        configureProject().assertBuildScriptCompiled().assertOutputContains("foobar")

        givenKotlinScriptInBuildSrcContains(
            "foo",
            """
            inline fun foo() = "bar"
            """,
            multifileAnnotations
        )
        givenKotlinScriptInBuildSrcContains(
            "bar",
            """
            inline fun bar() = "foo"
            """,
            multifileAnnotations
        )
        configureProject().assertBuildScriptBodyRecompiled().assertOutputContains("barfoo")
    }

    @Test
    fun `avoids buildscript recompilation on internal inline function changes in multifile class in buildSrc`() {
        val multifileAnnotations = """
            @file:JvmName("Utils")
            @file:JvmMultifileClass
        """
        val packageName = givenKotlinScriptInBuildSrcContains(
            "foo",
            """
            fun foo() = fooInternal()
            internal inline fun fooInternal() = "foo"
            """,
            multifileAnnotations
        )
        givenKotlinScriptInBuildSrcContains(
            "bar",
            """
            fun bar() = barInternal()
            internal inline fun barInternal() = "bar"
            """,
            multifileAnnotations
        )
        withUniqueScript("println($packageName.foo() + $packageName.bar())")
        configureProject().assertBuildScriptCompiled().assertOutputContains("foobar")

        givenKotlinScriptInBuildSrcContains(
            "foo",
            """
            fun foo() = fooInternal()
            internal inline fun fooInternal() = "bar"
            """,
            multifileAnnotations
        )
        givenKotlinScriptInBuildSrcContains(
            "bar",
            """
            fun bar() = barInternal()
            internal inline fun barInternal() = "foo"
            """,
            multifileAnnotations
        )
        configureProject().assertOutputContains("barfoo")
    }

    private
    fun buildJarForBuildScriptClasspath(classBody: String): Pair<String, String> {
        val baseDir = "buildscript"
        withDefaultSettingsIn(baseDir).appendText(
            """
                rootProject.name = "buildscript"
            """
        )
        withBuildScriptIn(
            baseDir,
            """
                plugins {
                    id("java-library")
                }
            """
        )
        val className = javaSourceFile(baseDir, classBody)
        build(existing(baseDir), "build")
        val jarPath = "$baseDir/build/libs/buildscript.jar"
        assertTrue(existing(jarPath).exists())
        return Pair(className, jarPath)
    }

    private
    fun buildKotlinJarForBuildScriptClasspath(@Suppress("SameParameterValue") classBody: String): Pair<String, String> {
        val baseDir = "buildscript"
        withDefaultSettingsIn(baseDir).appendText(
            """
                rootProject.name = "buildscript"
            """
        )
        withBuildScriptIn(
            baseDir,
            """
                plugins {
                    `kotlin-dsl`
                    id("java-library")
                }
                repositories {
                    mavenCentral()
                }
            """
        )
        val className = kotlinClassSourceFile(baseDir, classBody)
        build(existing(baseDir), "build")
        val jarPath = "$baseDir/build/libs/buildscript.jar"
        assertTrue(existing(jarPath).exists())
        return Pair(className, jarPath)
    }
}
