package komplex.sample

import komplex.*

fun main(args: Array<String>) = block {

    val test = configuration("tests")
    val jar = configuration("jar")

    shared {
        val testLibs = libraries {
            // collection of libraries
            library("junit")
            library("hamcrest", "1.2")
        }

        // shared settings for all projects
        version("SNAPSHOT-0.1")
        depends(test) on testLibs // depends in configuration

        building { println("Building $title...") }
        built { println("Done building $title.") }
    }

    project("spek") {
        shared("*-test") {
            // shared settings for projects matching pattern
            description("Applied to projects in $projectName with name ending with '-test'")
        }

        // variable denoting project
        val core = project("spek-core") {
            description("Spek Core")
            build using tools.kotlin from files("spek-core/src/**") to folder("out/spek-core")
            build(test) using tools.kotlin from files("spek-core/test/**") to folder("out/spek-core")
            build(jar) using tools.jar from folder("out/spek-core") to file("artifacts/spek-core.jar")
        }

        project("spek-tests") {
            description("Spek Tests")
            depends on core // reference to project with variable
            depends on library("mockito-all", "1.9.5", "org.mockito") // inline library dependence
        }

        project("spek-samples") {
            description("Spek Samples")
            depends on project("spek-core") // reference to project by name
        }
    }

}.dump()