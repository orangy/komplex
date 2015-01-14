package komplex.sample

import komplex.*
import komplex.jar.*
import komplex.kotlin.*
import komplex.maven.*
import kotlin.modules.module

fun main(args: Array<String>) {
    val script = script {
        /// BUILD SCRIPT
        val test = scenario("test")
        val jar = scenario("jar")
        val publish = scenario("publish")

        fun Module.shared() {
            version("SNAPSHOT-0.1")
            // \todo export/local steps

            // shared settings for all projects
            val sources = files("$moduleName/src/**.kt", artifacts.sources)
            val binaries = folder("out/sample/$moduleName", artifacts.binaries)
            val jarFile = file("out/artifacts/sample/$moduleName.jar", artifacts.jar)

            // invent syntax for "local/export" property passing
            val extLibs = build using(tools.maven) from depends.artifacts with {
                dir = "lib"
            }

            build using(tools.kotlin) from sources into binaries with {
                useLibs(extLibs)
                useLibs(modules.map { {(scenario: Scenario) -> it.targets(scenario).filter { it.type == artifacts.jar }}})
                useLibs({(scenario: Scenario) -> depends.modules(scenario).flatMap { it.targets(scenario).filter { it.type == artifacts.jar }}})
                enableInline = true
            }

            build(jar, test) using tools.jar from binaries into jarFile

            build(publish) {
                using(tools.jar) {
                    from(binaries)
                    into(jarFile)
                    compression = 2
                }
                using(tools.publish) {
                    from(jarFile)
                }
            }
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
                depends on core
                depends on {
                    library("org.jetbrains.kotlin:kotlin-compiler:0.10.195")
                    library("org.jetbrains.kotlin:kotlin-runtime:0.10.195")
                }
                shared()
            }
            val repoMaven = module("tools/maven", "Komplex Maven Resolver tool") {
                shared()
                depends on core
                depends on {
                    library("com.jcabi:jcabi-aether:0.10.1")
                    library("org.apache.maven:maven-core:3.2.5")
                }
            }

            module("samples", "Komplex Samples") {
                shared()
                depends on core // reference to project by name
                depends on toolsJar
                depends on toolsKotlin
                depends on repoMaven
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
    println("--- script ------------------------------")
    script.print("")
//    script.build("publish")
    val graph = script.buildGraph("publish")
//    println("graph targets:")
//    graph.targets().forEach { it.print("    ") }
//    println()
//    println("graph:")
//    graph.print()
    println()
    println("--- plan --------------------------------")
    graph.printBuildPlan()
    println()
    println("--- build -------------------------------")
    graph.build()
    println()
    println("-- done. --------------------------------")
//    script.print("")
}