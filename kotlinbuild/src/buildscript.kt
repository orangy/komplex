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

        val libraries = folder(rootDir.resolve("out/sample/libs"), artifacts.binaries)

        fun library(id: String, version: String? = null, scenario: Scenarios = Scenarios.Default_): Module {
            val libModule = komplex.tools.maven.mavenLibrary(id, version, target = libraries)
            libModule.build using tools.maven
            return libModule
        }

        fun Module.shared(kotlinSources: Iterable<Artifact>, kotlinSourceRoots: Iterable<String>, javaSources: Iterable<Artifact>) {
            version("ATTEMPT-0.1")

            // shared settings for all projects
            val kotlinBinaries = folder(rootDir.resolve("out/kb/build.kt/$moduleName"), artifacts.binaries)
            val javaBinaries = folder(rootDir.resolve("out/kb/build/$moduleName"), artifacts.binaries)
            val jarFile = file(rootDir.resolve("out/kb/artifacts/$moduleName.jar"), artifacts.jar)
            val libs = artifactsSet(
                    file(rootDir.resolve("dependencies/bootstrap-compiler/Kotlin/lib/kotlin-runtime.jar"), artifacts.jar),
                    file(rootDir.resolve("ideaSDK/lib/protobuf-2.5.0.jar"), artifacts.jar),
                    file(rootDir.resolve("dependencies/jline.jar"), artifacts.jar),
                    file(rootDir.resolve("dependencies/jansi.jar"), artifacts.jar),
                    file(rootDir.resolve("dependencies/cli-parser-1.1.1.jar"), artifacts.jar),
                    file(rootDir.resolve("ideaSDK/jps/jps-model.jar"), artifacts.jar),
                    files("*.jar", artifacts.jar, base = rootDir.resolve("ideaSDK/core")),
                    files("*.jar", artifacts.jar, base = rootDir.resolve("lib")),
                    files("**/*.jar", artifacts.jar, base = rootDir.resolve("lib"))
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
            }

            build(jar, test) using tools.jar from javaBinaries export jarFile

            build(publish) {
                using(tools.jar) {
                    from(javaBinaries)
                    into(jarFile)
                    compression = 2
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
                        kotlinSources = baseDirs.map { files("src/**.kt", artifacts.sources, base = it) },
                        kotlinSourceRoots = baseDirs.map { it + "/src" },
                        javaSources = baseDirs.map { files("src/**.java", artifacts.sources, base = it) })

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

