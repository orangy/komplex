package komplex.sample

import komplex.*
import komplex.jar.*
import komplex.kotlin.*
import komplex.dependencies.repository

fun main(args: Array<String>) {
    val script = script {
        /// BUILD SCRIPT
        val test = scenario("test")
        val jar = scenario("jar")
        val publish = scenario("publish")

        fun Module.shared() {
            version("SNAPSHOT-0.1")

            repository("lib") // dependencies should be placed here manually for now

            // shared settings for all projects
            val sources = files("$moduleName/src/**.kt", artifacts.sources)
            val binaries = folder("./out/sample/$moduleName", artifacts.binaries)
            val jarFile = file("./artifacts/sample/$moduleName.jar", artifacts.jar)

            build using(tools.kotlin) from sources into binaries with {
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
                shared()
                depends on core
            }
            val toolsKotlin = module("tools/kotlin", "Komplex Kotlin Compiler tool") {
                shared()
                depends on core
                depends on {
                    library("kotlin-compiler", null) // something wrong with dispatching, \todo check with kotlin team
                    library("kotlin-runtime")
                }
            }

            module("samples", "Komplex Samples") {
                shared()
                depends on core // reference to project by name
                depends on toolsJar
                depends on toolsKotlin
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
    script.print("")
    script.build("publish")
}