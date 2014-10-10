package komplex.kotlin

import komplex.*
import java.io.*
import org.jetbrains.jet.cli.common.messages.MessageCollector
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation
import org.jetbrains.jet.cli.common.ExitCode
import org.jetbrains.jet.cli.jvm.K2JVMCompiler
import org.jetbrains.jet.cli.common.arguments.K2JVMCompilerArguments
import komplex.dependencies.resolver

public val tools.kotlin: KotlinCompiler
    get() = KotlinCompiler()

public class KotlinCompiler : CompilerTool("Kotlin Compiler") {

    public fun invoke(body: KotlinCompiler.() -> Unit): KotlinCompiler {
        body()
        return this
    }

    public var enableInline: Boolean = true

    public override fun convert(context: BuildContext, from: List<Artifact>, to: List<Artifact>): BuildResult {
        val compiler = K2JVMCompiler()
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
        val project = context.module

        messageCollector.report(
                CompilerMessageSeverity.INFO,
                "compiling module ${project.moduleName} from $from to $to",
                CompilerMessageLocation.NO_LOCATION)

        args.printArgs = true
        args.src = from.getAllStreams().map { it.path } .joinToString(File.pathSeparator)

        val libraries = project.depends.dependencies
                .filter { it.scenario.matches(context.scenario) }
                .flatMap { resolver.resolve(it.reference, context.scenario) }
                .filterNotNull()
                .map { it.toString() }
                .joinToString(File.pathSeparator)

        //log.debug("classpath: {}", libraries)
        messageCollector.report(CompilerMessageSeverity.INFO, "build classpath: $libraries", CompilerMessageLocation.NO_LOCATION)

        args.classpath = libraries

        val folder = to.single()
        when (folder) {
            is FolderArtifact -> args.outputDir = folder.path.toString()
            else -> throw IllegalArgumentException("Compiler only supports single folder as destination")
        }

        val exitCode = compiler.exec(messageCollector, args)
        return when (exitCode) {
            ExitCode.OK -> BuildResult.Success
            else -> BuildResult.Fail
        }
    }
}
