package komplex.sample

import komplex.*
import komplex.jar.*
import komplex.kotlin.*

fun main(args: Array<String>) = block {

    val test = config("test")
    val jar = config("jar")
    val publish = config("publish")

    val testLibs = libraries {
        // collection of libraries
        library("junit", "4.11")
        library("hamcrest-core", "1.3")
    }

    repository {

    }

    shared {
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

            depends on core // reference to project with variable
            depends on library("mockito-all", "1.9.5", "org.mockito") // inline library dependence

            build(test) using tools.kotlin from sources to binaries
        }

        project("spek-samples", "Samples") {
            val sources = files("spek-samples/src/**")
            val binaries = folder("out/spek-samples")

            depends on project("spek-core") // reference to project by name

            build using tools.kotlin from sources to binaries
        }

        project("spek-core", "Core") {
            val sources = files("spek-core/src/**")
            val binaries = folder("out/spek-core")
            val jarFile = file("artifacts/spek-core.jar")

            depends on testLibs

            build using(tools.kotlin) from sources to binaries
            build(jar, publish) using tools.jar from binaries to jarFile
            build(publish) using tools.publish from jarFile
        }
    }

}.build("publish")