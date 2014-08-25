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

        shared {
            version("SNAPSHOT-0.1")

            // shared settings for all projects
            val sources = files("$moduleName/src/**")
            val binaries = folder("out/$moduleName")
            val jarFile = file("artifacts/$moduleName.jar")

            build using(tools.kotlin) from sources to binaries
            build(jar, test, publish) using tools.jar from binaries to jarFile
            build(publish) using tools.publish from jarFile
            build(test) using tools.junit from jarFile

            depends on module("junit") {
                module from maven("junit", "4.11")
                module from maven("hamcrest-core", "1.3")
            }

            building { println("Building $title...") }
            built { println("Done building $title.") }
        }

        module("spek") {
            val core = module("spek-core", "Spek Core") {
                build(binDep) using tools.maven from mavenRepo
            }

            module("spek-samples", "Spek Samples") {
                depends on core // reference to project by name
            }

            module("spek-tests", "Spek Tests") {
                depends on core // reference to project with variable
                depends on (module from maven("mockito-all", "1.9.5", "org.mockito")) // inline library dependence
            }
        }
        /// BUILD SCRIPT
    }

    // kbuild script.build -t publish -t src
    project.build("publish src")

}