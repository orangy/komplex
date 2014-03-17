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

}

fun file(path: String) = Files().let { it.include(path); it }
fun files(path: String) = Files().let { it.include(path); it }
fun folder(path: String) = Files().let { it.include(path); it }

class Build() {

    val processes = ArrayList<BuildProcess>()

    fun invoke(vararg config: Configuration): Build {
        return this
    }

    fun using(tool: Tool): BuildProcess {
        val process = BuildProcess(tool)
        processes.add(process)
        return process
    }

    fun dump(indent: String = "") {
        println("$indent Build:")
        for (process in processes) {
            process.dump(indent + "  ")
        }

    }
}