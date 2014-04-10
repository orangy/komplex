package komplex.kotlin

import komplex.*
import org.jetbrains.jet.cli.jvm.K2JVMCompiler
import org.jetbrains.jet.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.jet.cli.common.messages.MessageCollector
import java.io.*
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation
import org.jetbrains.jet.cli.common.ExitCode

val tools.kotlin : KotlinCompiler
        get() = KotlinCompiler()

class KotlinCompiler : Tool("Kotlin Compiler") {

    fun invoke(body: KotlinCompiler.() -> Unit): KotlinCompiler {
        body()
        return this
    }

    override fun execute(process: BuildProcess, from: List<BuildEndPoint>, to: List<BuildEndPoint>): BuildResult {
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
        args.verbose = true
        args.tags = true
        args.src = from.getAllStreams().map { it.path } .makeString(File.pathSeparator)

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
