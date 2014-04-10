package komplex.sample

import komplex.*
import komplex.jar.*
import komplex.kotlin.*

fun main(args: Array<String>) = block {
    val test = config("test")
    val jar = config("jar")
    val publish = config("publish")

    shared {
        version("SNAPSHOT-0.1")

        // shared settings for all projects
        val sources = files("$projectName/src/**")
        val binaries = folder("out/$projectName")
        val jarFile = file("artifacts/$projectName.jar")

        build using(tools.kotlin) from sources to binaries
        build(jar, publish) using tools.jar from binaries to jarFile
        build(publish) using tools.publish from jarFile

        depends on libraries {
            // collection of libraries
            library("junit", "4.11")
            library("hamcrest-core", "1.3")
        }

        building { println("Building $title...") }
        built { println("Done building $title.") }
    }

    project("spek") {
        val core = project("spek-core", "Spek Core") {
        }

        project("spek-samples", "Spek Samples") {
            depends on core // reference to project by name
        }

        project("spek-tests", "Spek Tests") {
            depends on core // reference to project with variable
            depends on library("mockito-all", "1.9.5", "org.mockito") // inline library dependence
        }
    }
}.build()