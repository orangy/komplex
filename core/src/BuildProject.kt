package komplex

import java.util.ArrayList

trait BuildEndPoint {
    fun dump(indent: String = "")
}

class BuildProcess(val buildConfiguration : BuildConfiguration, val tool: Tool) {
    val sources = arrayListOf<BuildEndPoint>()
    val destinations = arrayListOf<BuildEndPoint>()

    fun from(vararg endpoints: BuildEndPoint): BuildProcess {
        sources.addAll(endpoints)
        return this
    }

    fun to(vararg endpoints: BuildEndPoint): BuildProcess {
        destinations.addAll(endpoints)
        return this
    }

    fun dump(indent: String = "") {
        println("$indent ${tool.title}")

        println("$indent   From: ")
        for (endpoint in sources)
            endpoint.dump(indent + "    ")
        println("$indent   To: ")
        for (endpoint in destinations)
            endpoint.dump(indent + "    ")
    }

    val started = Event<BuildProcess>("Started")
    val finished = Event<BuildProcess>("Finished")

    fun execute() {
        started.fire(this)
        tool.execute(sources, destinations)
        finished.fire(this)
    }
}

class BuildConfiguration(val buildProject: BuildProject, val configurations : List<Config>) {
    val processes = ArrayList<BuildProcess>()

    fun invoke(body : BuildConfiguration.()->Unit) {
        body()
    }

    fun using(tool: Tool): BuildProcess {
        val process = BuildProcess(this, tool)
        processes.add(process)
        return process
    }

    fun dump(indent: String = "") {
        println("$indent Build $configurations")
        for (process in processes) {
            process.dump(indent + "  ")
        }
    }
}

class BuildProject(val project : Project) {
    val configurations = ArrayList<BuildConfiguration>()

    fun using(tool: Tool): BuildProcess = invoke(Config("*")).using(tool)

    fun invoke(vararg config: Config): BuildConfiguration {
        val build = BuildConfiguration(this, config.toList())
        configurations.add(build)
        return build
    }

    fun dump(indent: String = "") {
        for (builds in configurations) {
            builds.dump(indent)
        }
    }
}