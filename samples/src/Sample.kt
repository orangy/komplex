package komplex.sample

import komplex.dsl.*
import komplex.model.*
import komplex.tools.classpath
import komplex.tools.jar.jar
import komplex.tools.kotlin.kotlin
import komplex.tools.maven.maven
import komplex.utils
import java.nio.file.Paths

fun main(args: Array<String>) {
    val script = script {
        /// BUILD SCRIPT
        val test = scenario("test")
        val jar = scenario("jar")
        val publish = scenario("publish")

        val libraries = folder(artifacts.binary, "out/sample/libs")

        fun library(id: String, version: String? = null, scenario: Scenarios = Scenarios.Default_): komplex.tools.maven.MavenLibraryModule {
            val libModule = komplex.tools.maven.mavenLibrary(id, version, target = libraries)
            return libModule
        }

        val slf4j = library("org.slf4j:slf4j-api:1.7.9")
        val kotlinCompiler = library("org.jetbrains.kotlin:kotlin-compiler:0.12.213")
        val kotlinRuntime = library("org.jetbrains.kotlin:kotlin-runtime:0.12.213")
        val kotlinRreflect = library("org.jetbrains.kotlin:kotlin-reflect:0.12.213")

        fun ProjectModule.shared() {
            version("SNAPSHOT-0.1")

            // shared settings for all projects
            val sources = files(artifacts.source, ".", "$moduleName/src/**/*.kt", "$moduleName/src/*.kt")
            val binaries = folder(artifacts.binary, "out/sample/$moduleName")
            val jarFile = file(artifacts.jar, "out/artifacts/sample/$moduleName.jar")

            depends on children

            build using(tools.kotlin(Paths.get("lib/kotlin-compiler.jar"))) from sources into binaries with {
                depends.on( kotlinRuntime, kotlinRreflect)
                classpath(depends.modules)
                enableInline = true
            }

            build(jar, test) using tools.jar from binaries into jarFile

//            build(publish) {
//                using(tools.jar) {
//                    from(binaries)
//                    into(jarFile)
//                    deflate = true
//                }
//                using(tools.publish) {
//                    from(jarFile)
//                }
//            }

            default(jar) // default build scenario, '*'/null if not specified (means - all)
        }

        module("komplex", rootPath = Paths.get(".")) {

            val core = module("core", "Komplex Core") {
                depends.on( slf4j )
                shared()
            }

            val toolsJar = module("tools/jar", "Komplex jar tool") {
                depends.on(
                        core, // reference to project by name
                        slf4j
                )
                shared()
            }
            val toolsProGuard = module("tools/proguard", "Komplex proguard tool") {
                depends.on(
                        core,
                        library("net.sf.proguard:proguard-base:5.2.1"),
                        slf4j
                )
                shared()
            }
            val toolsJavac = module("tools/javac", "Komplex Java Compiler tool") {
                depends.on(
                        core,
                        kotlinRuntime,
                        slf4j
                )
                shared()
            }
            val toolsJavascript = module("tools/javascript", "Komplex JavaScript tool") {
                depends.on(
                        core,
                        kotlinRuntime,
                        library("com.google.javascript:closure-compiler:v20150505"),
                        slf4j
                )
                shared()
            }
            val toolsKotlin = module("tools/kotlin", "Komplex Kotlin Compiler tool") {
                depends.on(
                        core,
                        toolsJavac,
                        kotlinCompiler,
                        kotlinRuntime,
                        slf4j
                )
                shared()
            }
            val repoMaven = module("tools/maven", "Komplex Maven Resolver tool") {
                depends.on(
                        core,
                        library("com.jcabi:jcabi-aether:0.10.1"),
                        library("org.apache.maven:maven-core:3.2.5"),
                        slf4j
                )
                shared()
            }

            module("samples", "Komplex Samples") {
                depends.on(
                        core,
                        toolsJar,
                        toolsKotlin,
                        repoMaven,
                        slf4j,
                        library("org.slf4j:slf4j-simple:1.7.9")
                )
                shared()
            }
/*
            module("tests", "Komplex Tests") {
                shared()
                depends on core // reference to project with variable
                depends on {
                    library("junit", "4.11")
                    library("hamcrest-core", "1.3")
                }
            }
*/
        }
        /// BUILD SCRIPT
    }

    // kbuild script.build -t publish -t src

    println("\n--- script ------------------------------")
    println(script.nicePrint(utils.TwoSpaceIndentLn()))
    val graph = script.buildGraph()
    println("\n--- plan --------------------------------")
    println(graph.nicePrint( utils.TwoSpaceIndentLn(),  Scenarios.All))
    println("\n--- build -------------------------------")
    graph.build(Scenarios.All)
    println("\n--- build 2 - partial -------------------")
    graph.build(Scenarios.All, GraphBuildContext(Scenarios.All, graph))
    println("\n-- done. --------------------------------")
}