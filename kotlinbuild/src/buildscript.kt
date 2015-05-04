package kotlin.buildscript

import komplex.dsl.*
import komplex.dsl.tools
import komplex.dsl.Module
import komplex.tools.jar.jar
import komplex.tools.kotlin.kotlin
import komplex.tools.maven.maven
import komplex.model.*
import komplex.tools.javac.javac
import komplex.tools.use
import komplex.utils
import java.nio.file.Path
import java.nio.file.Paths

fun main(args: Array<String>) {

    // \todo detect root
    val rootDir = Paths.get(args.first())

    val script = script {
        /// BUILD SCRIPT
        val test = scenario("test")
        val jar = scenario("jar")
        val publish = scenario("publish")

        val libraries = folder(artifacts.binaries, rootDir.resolve("out/sample/libs"))

        fun library(id: String, version: String? = null, scenario: Scenarios = Scenarios.Default_): Module {
            val libModule = komplex.tools.maven.mavenLibrary(id, version, target = libraries)
            libModule.build using tools.maven
            return libModule
        }

        fun Module.shared(kotlinSources: Iterable<Artifact>, kotlinSourceRoots: Iterable<String>, javaSources: Iterable<Artifact>) {
            version("ATTEMPT-0.1")

            // shared settings for all projects
            val kotlinBinaries = folder(artifacts.binaries, rootDir.resolve("out/kb/build.kt/$moduleName"))
            val javaBinaries = folder(artifacts.binaries, rootDir.resolve("out/kb/build/$moduleName"))
            val jarFile = file(artifacts.jar, rootDir.resolve("out/kb/artifacts/kotlin-$moduleName.jar"))
            val libs = artifactsSet(
                    file(artifacts.jar, rootDir.resolve("dependencies/bootstrap-compiler/Kotlin/lib/kotlin-runtime.jar")),
                    file(artifacts.jar, rootDir.resolve("ideaSDK/lib/protobuf-2.5.0.jar")),
                    file(artifacts.jar, rootDir.resolve("dependencies/jline.jar")),
                    file(artifacts.jar, rootDir.resolve("dependencies/cli-parser-1.1.1.jar")),
                    file(artifacts.jar, rootDir.resolve("ideaSDK/jps/jps-model.jar")),
                    files(artifacts.jar, rootDir.resolve("ideaSDK/core"), "*.jar"),
                    files(artifacts.jar, rootDir.resolve("lib"), "*.jar"),
                    files(artifacts.jar, rootDir.resolve("lib"), "**/*.jar")
            )
            val jarContent = artifactsSet(
                    files(artifacts.jar, rootDir.resolve("lib"), "*.jar"),
                    files(artifacts.jar, rootDir.resolve("ideaSDK/core"), "*.jar").exclude("util.jar"),
                    file(artifacts.jar, rootDir.resolve("ideaSDK/jps/jps-model.jar")),
                    file(artifacts.jar, rootDir.resolve("ideaSDK/lib/jna-utils.jar")),
                    file(artifacts.jar, rootDir.resolve("ideaSDK/lib/oromatcher.jar")),
                    file(artifacts.jar, rootDir.resolve("ideaSDK/lib/protobuf-2.5.0.jar")),
                    file(artifacts.jar, rootDir.resolve("dependencies/jline.jar")),
                    file(artifacts.jar, rootDir.resolve("dependencies/cli-parser-1.1.1.jar")),
                    files(artifacts.sources, rootDir.resolve("compiler/frontend.java/src"), "META-INF/services/**"),
                    files(artifacts.sources, rootDir.resolve("compiler/backend/src"), "META-INF/services/**"),
                    files(artifacts.sources, rootDir.resolve("resources"), "kotlinManifest.properties"),
                    files(artifacts.sources, rootDir.resolve("idea/src"), "META-INF/extensions/common.xml"),
                    files(artifacts.sources, rootDir.resolve("idea/src"), "META-INF/extensions/kotlin2jvm.xml"),
                    files(artifacts.sources, rootDir.resolve("idea/src"), "META-INF/extensions/kotlin2js.xml")
            )

            depends on children

            build using(tools.kotlin) from kotlinSources into kotlinBinaries with {
                use(depends.modules)
                use(libs)
                sourceRoots.addAll(kotlinSourceRoots)
                enableInline = true
            }

            build using(tools.javac) from javaSources into javaBinaries with {
                use(depends.modules)
                use(kotlinBinaries)
                use(libs)
            }

            build(jar, test) using tools.jar with {
                from(kotlinBinaries, javaBinaries, jarContent)
                export(jarFile)
                deflate = true
            }

            build(publish) {
                using(tools.jar) {
                    from(kotlinBinaries, javaBinaries, libs)
                    into(jarFile)
                    deflate = true
                }
                /*
                using(tools.publish) {
                    from(jarFile)
                }
                */
            }

            default(jar) // default build scenario, '*'/null if not specified (means - all)
        }

        fun Module.makeFrom(vararg baseDirs: String) =
                this.shared(
                        kotlinSources = baseDirs.map { files(artifacts.sources, rootDir.resolve(it), "src/**.kt") },
                        kotlinSourceRoots = baseDirs.map { it + "/src" },
                        javaSources = baseDirs.map { files(artifacts.sources, rootDir.resolve(it), "src/**.java") })

        module("kotlin") {
            val compoler = module("compiler", "Kotlin Compiler") {
                depends.on(
                        library("org.slf4j:slf4j-api:1.7.12"),
                        library("org.jetbrains:annotations:13.0")
                )
                makeFrom( "core/descriptor.loader.java",
                          "core/descriptors",
                          "core/deserialization",
                          "core/util.runtime",
                          "compiler/backend",
                          "compiler/backend-common",
                          "compiler/builtins-serializer",
                          "compiler/cli",
                          "compiler/cli/cli-common",
                          "compiler/frontend",
                          "compiler/frontend.java",
                          "compiler/light-classes",
                          "compiler/plugin-api",
                          "compiler/serialization",
                          "compiler/util",
                          "js/js.dart-ast",
                          "js/js.translator",
                          "js/js.frontend",
                          "js/js.inliner",
                          "js/js.parser",
                          "js/js.serializer"
                )
            }
        }
        /// BUILD SCRIPT
    }

    println("\n--- script ------------------------------")
    println(script.nicePrint(utils.TwoSpaceIndentLn()))
    val graph = script.buildGraph()
    println("\n--- plan --------------------------------")
    println(graph.nicePrint( utils.TwoSpaceIndentLn(),  Scenarios.All))
    println("\n--- build -------------------------------")
    graph.build(Scenarios.All)
    println("\n-- done. --------------------------------")
}

