package komplex.kotlin

import komplex.*
import java.io.*

public val tools.kotlin: KotlinCompiler
    get() = KotlinCompiler()

public class KotlinCompiler : CompilerTool("Kotlin Compiler") {

    public fun invoke(body: KotlinCompiler.() -> Unit): KotlinCompiler {
        body()
        return this
    }

    public var enableInline: Boolean = true

    public override fun convert(context: BuildContext, from: List<Artifact>, to: List<Artifact>): BuildResult {
/*        val compiler = K2JVMCompiler()
        val args = K2JVMCompilerArguments()
        val messageCollector = object : MessageCollector {
            override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation) {
                if (severity == CompilerMessageSeverity.OUTPUT)
                    return

                if (location == CompilerMessageLocation.NO_LOCATION) {
                    println("[$severity] $message")
                } else {
                    println("[$severity] $message ($location)")
                }
            }
        }

        args.printArgs = true
        args.src = from.getAllStreams().map { it.path } .makeString(File.pathSeparator)

        val project = context.module
        val repository = project.repository
        val libraries = project.depends.libraries
                .filter { it.config.matches(context.config) }
                .map { repository.resolve(it.reference) }
                .filterNotNull()
                .map { it.classPath }
                .makeString(File.pathSeparator)

        args.classpath = libraries

        val folder = to.single()
        when (folder) {
            is FolderEndPoint -> args.outputDir = folder.path.toString()
            else -> throw IllegalArgumentException("Compiler only supports single folder as destination")
        }

        val exitCode = compiler.exec(messageCollector, args)
        return when (exitCode) {
            ExitCode.OK -> BuildResult.Success
            else -> BuildResult.Fail

        }*/
        return BuildResult.Success
    }
}
