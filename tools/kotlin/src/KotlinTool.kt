package komplex.kotlin

import komplex.*
import org.jetbrains.jet.cli.jvm.*
import org.jetbrains.jet.cli.common.arguments.*
import java.io.*
import org.jetbrains.jet.cli.common.messages.*
import org.jetbrains.jet.cli.common.*

val tools.kotlin: KotlinCompiler
    get() = KotlinCompiler()

class KotlinCompiler : CompilerTool("Kotlin Compiler") {

    fun invoke(body: KotlinCompiler.() -> Unit): KotlinCompiler {
        body()
        return this
    }

    override fun execute(context: BuildContext, from: List<BuildEndPoint>, to: List<BuildEndPoint>): BuildResult {
        val compiler = K2JVMCompiler()
        val args = K2JVMCompilerArguments()
        val messageCollector = object : MessageCollector {
            override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation) {
                if (location == CompilerMessageLocation.NO_LOCATION) {
                    println("[$severity] $message")
                } else {
                    println("[$severity] $message ($location)")
                }
            }
        }

        args.printArgs = true
        args.src = from.getAllStreams().map { it.path } .makeString(File.pathSeparator)

        val project = context.project
        val repository = project.repository
        val classPath = project.depends.libraries
                .filter { it.config.matches(context.config) }
                .map { repository.resolve(it.reference) }
                .filterNotNull()
                .map { it.classPath }
                .makeString(File.pathSeparator)

        args.classpath = classPath

        val folder = to.single()
        when (folder) {
            is BuildFolder -> args.outputDir = folder.path.toString()
            else -> throw IllegalArgumentException("Compiler only supports folders as destinations")
        }

        val exitCode = compiler.exec(messageCollector, args)
        return when (exitCode) {
            ExitCode.OK -> BuildResult.Success
            else -> BuildResult.Fail

        }
    }
}
