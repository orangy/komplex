package komplex.sample

import komplex.*
import komplex.jar.*
import komplex.kotlin.*

fun main(args: Array<String>) {
    val script = script {
        /// BUILD SCRIPT
        val test = scenario("test")
        val jar = scenario("jar")
        val publish = scenario("publish")

        fun Module.shared() {
            version("SNAPSHOT-0.1")

            // shared settings for all projects
            val sources = files("$moduleName/src/**", artifacts.sources)
            val binaries = folder("out/$moduleName", artifacts.binaries)
            val jarFile = file("artifacts/$moduleName.jar", artifacts.jar)

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

            depends on module("junit") {
                module("junit", "4.11") {}
                module("hamcrest-core", "1.3") {}
            }
        }

        module("spek") {
            val core = module("spek-core", "Spek Core") {
                shared()
            }

            module("spek-samples", "Spek Samples") {
                shared()
                depends on core // reference to project by name
            }

            module("spek-tests", "Spek Tests") {
                shared()
                depends on core // reference to project with variable
            }
        }
        /// BUILD SCRIPT
    }

    // kbuild script.build -t publish -t src
    script.print("")
}