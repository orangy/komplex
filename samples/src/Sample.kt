package komplex.sample

import komplex.dsl.*
import komplex.dsl.Module
import komplex.tools.jar.*
import komplex.kotlin.*
import komplex.maven.*
import komplex.model.*
import komplex.tools.use
import komplex.utils

fun main(args: Array<String>) {
    val script = script {
        /// BUILD SCRIPT
        val test = scenario("test")
        val jar = scenario("jar")
        val publish = scenario("publish")

        val libraries = folder("out/sample/libs", artifacts.binaries)

        fun library(id: String, version: String? = null, scenario: Scenarios = Scenarios.Default_): Module {
            val libModule = komplex.maven.mavenLibrary(id, version, target = libraries)
            libModule.build using tools.maven
            return libModule
        }

        fun Module.shared() {
            version("SNAPSHOT-0.1")

            // shared settings for all projects
            val sources = files("$moduleName/src/**.kt", artifacts.sources)
            val binaries = folder("out/sample/$moduleName", artifacts.binaries)
            val jarFile = file("out/artifacts/sample/$moduleName.jar", artifacts.jar)

            depends on children

            build using(tools.kotlin) from sources into binaries with {
                use(depends.modules)
                enableInline = true
            }

            build(jar, test) using tools.jar from binaries export jarFile

            build(publish) {
                using(tools.jar) {
                    from(binaries)
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

        module("komplex") {
            val core = module("core", "Komplex Core") {
                shared()
            }

            val toolsJar = module("tools/jar", "Komplex jar tool") {
                depends on core
                shared()
            }
            val toolsKotlin = module("tools/kotlin", "Komplex Kotlin Compiler tool") {
                depends.on(
                    core,
                    library("org.jetbrains.kotlin:kotlin-compiler:0.11.91"),
                    library("org.jetbrains.kotlin:kotlin-runtime:0.11.91")
                )
                shared()
            }
            val repoMaven = module("tools/maven", "Komplex Maven Resolver tool") {
                depends on core
                depends.on(
                    library("com.jcabi:jcabi-aether:0.10.1"),
                    library("org.apache.maven:maven-core:3.2.5")
                )
                shared()
            }

            module("samples", "Komplex Samples") {
                depends on core // reference to project by name
                depends on toolsJar
                depends on toolsKotlin
                depends on repoMaven
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
//    println("\n--- min rebuild plan --------------------")
//    graph.printPartialBuildPlan(sources = hashSetOf(graph.roots().first()))
//    println("\n--- target build plan -------------------")
//    graph.printPartialBuildPlan(targets = listOf(graph.leafs().first()))
    println("\n-- done. --------------------------------")
}