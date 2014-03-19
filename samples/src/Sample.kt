package komplex.sample

import komplex.*

fun main(args: Array<String>) = block {

    val test = config("tests")
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
        // variable denoting project
        val core = project("spek-core", "Core") {
            val sources = files("spek-core/src/**")
            val binaries = folder("out/spek-core")

            build using tools.kotlin from sources to binaries
            build(jar) using tools.jar from binaries to file("artifacts/spek-core.jar")
        }

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
    }

}.build()