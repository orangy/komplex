package komplex

fun main(args: Array<String>) = block {
    project("spek") {
        val production = configuration("production") {
        }
        val test = configuration("tests") {
        }

        val testLibs = libraries {
            // collection of libraries
            library("junit")
            library("hamcrest", "1.2")
        }

        share {
            // shared settings for all projects
            version("SNAPSHOT-0.1")
            description("Configuration applied to all projects in $projectName")
            depends(test) on testLibs // depends in configuration

            building {
                println("Building $title...")
                // send timcity some notes as well
            }
            built {
                println("Done building $title.")
            }
        }

        share("*-test") {
            // shared settings for projects matching pattern
            description("Configuration applied to projects with name ending with '-test'")

        }

        // variable denoting project
        val core = project("spek-core") {
            description("Spek Core")
            // default sources are spek-core/src
            // default test sources are spek-core/tests
            building {
                println("NO NOISE!")
            }
            built {
                println("Relax...")
            }

            input {
                include("spek-core/src/**")
            }
            output {
                include("out/spek-core/**")
            }
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