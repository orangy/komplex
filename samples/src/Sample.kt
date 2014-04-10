package komplex.sample

import komplex.*
import komplex.jar.*
import komplex.system.*
import komplex.kotlin.*

fun main(args: Array<String>) = block {

    val test = config("test")
    val jar = config("jar")

    shared {
        val testLibs = libraries {
            // collection of libraries
            library("junit")
            library("hamcrest", "1.2")
        }

        // shared settings for all projects
        version("SNAPSHOT-0.1")
        depends(test) on testLibs // depends in configuration

        description("Spek " + description) // update description

        building { println("Building $title...") }
        built { println("Done building $title.") }
    }

    project("spek") {
        val core = project("spek-core") // forward declaration

        project("spek-tests", "Tests") {
            val sources = files("spek-core/test/**")
            val binaries = folder("out/spek-core")
            build(test) using tools.kotlin from sources to binaries
            depends on core // reference to project with variable
            depends on library("mockito-all", "1.9.5", "org.mockito") // inline library dependence
        }

        project("spek-samples", "Samples") {
            val sources = files("spek-samples/src/**")
            val binaries = folder("out/spek-samples")
            build using tools.kotlin from sources to binaries
            depends on project("spek-core") // reference to project by name
        }

        // variable denoting project
        project("spek-core", "Core") {
            val sources = files("spek-core/src/**")
            val binaries = folder("out/production/spek-core")

            val process = build using(tools.kotlin) from sources to binaries
            process.started {
                println("Compiling core...")
            }
            process.finished {
                println("Finished core.")
            }

            val jarFile = file("out/artifacts/spek-core.jar")
            build(jar) {
                using(tools.jar).from(binaries).to(jarFile)
                using(tools.publish).from(jarFile)
            }
        }
    }

}.build("jar")