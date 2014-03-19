package komplex

import java.util.ArrayList

class BuildProcess(val tool: Tool) {
    val from = Files()
    val to = Files()

    fun from(files: Files): BuildProcess {
        from.append(files)
        return this
    }

    fun to(files: Files): BuildProcess {
        to.append(files)
        return this
    }

    fun dump(indent: String = "") {
        println("$indent ${tool.title}")
        println("$indent   From: ")
        from.dump(indent + "    ")
        println("$indent   To: ")
        to.dump(indent + "    ")
    }

    fun execute() {
        tool.execute(from, to)
    }
}

fun file(path: String) = Files().let { it.include(path); it }
fun files(path: String) = Files().let { it.include(path); it }
fun folder(path: String) = Files().let { it.include(path); it }

class BuildStep(val configurations : List<Config>) {
    val processes = ArrayList<BuildProcess>()

    fun invoke(body : BuildStep.()->Unit) {
        body()
    }

    fun using(tool: Tool): BuildProcess {
        val process = BuildProcess(tool)
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

class Builds() {
    val steps = ArrayList<BuildStep>()

    fun using(tool: Tool): BuildProcess = invoke(Config("*")).using(tool)

    fun invoke(vararg config: Config): BuildStep {
        val build = BuildStep(config.toList())
        steps.add(build)
        return build
    }

    fun dump(indent: String = "") {
        for (builds in steps) {
            builds.dump(indent)
        }
    }
}