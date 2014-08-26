package komplex.sample

import komplex.*
import komplex.jar.*
import komplex.kotlin.*

fun main(args: Array<String>) {
    val project = project {
        /// BUILD SCRIPT
        val test = config("test")
        val jar = config("jar")
        val publish = config("publish")
        val srcDep = config("src")
        val binDep = config("bin")

        fun Module.shared() {
            version("SNAPSHOT-0.1")

            // shared settings for all projects
            val sources = files("$moduleName/src/**")
            val binaries = folder("out/$moduleName")
            val jarFile = file("artifacts/$moduleName.jar")

            build using(tools.kotlin) from sources to binaries
            build(jar, test) using tools.jar from binaries to jarFile

            build(publish) {
                using(tools.jar) {
                    from(binaries)
                    to(jarFile)

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
    project.dump("")

}