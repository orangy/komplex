package komplex.sample

import komplex.dsl.*
import komplex.model.GraphBuildContext
import komplex.model.Scenarios
import komplex.model.build
import komplex.model.nicePrint
import komplex.tools.classpath
import komplex.tools.jar.jar
import komplex.tools.kotlin.kotlin
import komplex.tools.maven.maven
import komplex.utils

fun main(args: Array<String>) {
    val script = script {
        /// BUILD SCRIPT
        val test = scenario("test")
        val jar = scenario("jar")
        val publish = scenario("publish")

        val libraries = folder(artifacts.binaries, "out/sample/libs")

        fun library(id: String, version: String? = null, scenario: Scenarios = Scenarios.Default_): Module {
            val libModule = komplex.tools.maven.mavenLibrary(id, version, target = libraries)
            libModule.build using tools.maven
            return libModule
        }

        fun Module.shared() {
            version("SNAPSHOT-0.1")

            // shared settings for all projects
            val sources = files(artifacts.sources).include("$moduleName/src/**/*.kt", "$moduleName/src/*.kt")
            val binaries = folder(artifacts.binaries, "out/sample/$moduleName")
            val jarFile = file(artifacts.jar, "out/artifacts/sample/$moduleName.jar")

            depends on children

            build using(tools.kotlin) from sources into binaries with {
                classpath(depends.modules, file(artifacts.jar, "lib/kotlin-runtime.jar"))
                enableInline = true
            }

            build(jar, test) using tools.jar from binaries export jarFile

            build(publish) {
                using(tools.jar) {
                    from(binaries)
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

        module("komplex") {
            val core = module("core", "Komplex Core") {
                depends.on(
                    library("org.slf4j:slf4j-api:1.7.9")
                )
                shared()
            }

            val toolsJar = module("tools/jar", "Komplex jar tool") {
                depends.on(
                    core, // reference to project by name
                    library("org.slf4j:slf4j-api:1.7.9")
                )
                shared()
            }
            val toolsProGuard = module("tools/proguard", "Komplex proguard tool") {
                depends.on(
                        core,
                        library("net.sf.proguard:proguard-base:5.2.1"),
                        library("org.slf4j:slf4j-api:1.7.9")
                )
                shared()
            }
            val toolsJavac = module("tools/javac", "Komplex Java Compiler tool") {
                depends.on(
                        core,
                        library("org.jetbrains.kotlin:kotlin-runtime:0.11.91"),
                        library("org.slf4j:slf4j-api:1.7.9")
                )
                shared()
            }
            val toolsKotlin = module("tools/kotlin", "Komplex Kotlin Compiler tool") {
                depends.on(
                    core,
                    library("org.jetbrains.kotlin:kotlin-compiler:0.11.91"),
                    library("org.jetbrains.kotlin:kotlin-runtime:0.11.91"),
                    library("org.slf4j:slf4j-api:1.7.9")
                )
                shared()
            }
            val repoMaven = module("tools/maven", "Komplex Maven Resolver tool") {
                depends.on(
                    core,
                    library("com.jcabi:jcabi-aether:0.10.1"),
                    library("org.apache.maven:maven-core:3.2.5"),
                    library("org.slf4j:slf4j-api:1.7.9")
                )
                shared()
            }

            module("samples", "Komplex Samples") {
                depends.on(
                    core,
                    toolsJar,
                    toolsKotlin,
                    repoMaven,
                    library("org.slf4j:slf4j-api:1.7.9"),
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
/*
    script.prepare()
    val plan = script.plan(script.findModule("spek-tests")!!, Scenario("publish"))
    plan.print("")
*/
    println("\n--- script ------------------------------")
    println(script.nicePrint(utils.TwoSpaceIndentLn()))
//    script.build("publish")
    val graph = script.buildGraph()
//    println("graph targets:")
//    graph.targets().forEach { it.print("    ") }
//    println()
//    println("graph:")
//    graph.print()
    println("\n--- plan --------------------------------")
    println(graph.nicePrint( utils.TwoSpaceIndentLn(),  Scenarios.All))
    println("\n--- build -------------------------------")
    graph.build(Scenarios.All)
    println("\n--- build 2 - partial -------------------")
    graph.build(Scenarios.All, GraphBuildContext(Scenarios.All, graph))
//    println("\n--- min rebuild plan --------------------")
//    graph.printPartialBuildPlan(sources = hashSetOf(graph.roots().first()))
//    println("\n--- target build plan -------------------")
//    graph.printPartialBuildPlan(targets = listOf(graph.leafs().first()))
    println("\n-- done. --------------------------------")
}