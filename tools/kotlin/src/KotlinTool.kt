package komplex.kotlin

import komplex.*
import java.io.*
import org.jetbrains.jet.cli.common.messages.MessageCollector
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation
import org.jetbrains.jet.cli.common.ExitCode
import org.jetbrains.jet.cli.jvm.K2JVMCompiler
import org.jetbrains.jet.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.jet.config.Services
import java.lang
import kotlin.properties.Delegates
import java.nio.file.Paths
import java.nio.file.Path


public val tools.kotlin: KotlinCompilerRule
    get() = KotlinCompilerRule()


// separate class for separate class loading
// \todo check if moving to separate file or jar is needed for really lazy tool loading, or may be that nested class will work as well
public class KotlinCompilerRule(override val export: Boolean = false) : komplex.Compiler.BaseRule(export) {
    override val tool by Delegates.lazy { KotlinCompiler() }
    override fun execute(context: BuildStepContext): BuildResult
            = tool.compileKotlin(context, selectSources.get(context.scenario), selectTargets.get(context.scenario), selectLibs.get(context.scenario), this)

    public fun invoke(body: KotlinCompilerRule.() -> Unit): KotlinCompilerRule {
        body()
        return this
    }
    public var enableInline: Boolean = true
}


public class KotlinCompiler : komplex.Compiler("Kotlin Compiler") {

    override fun compile(context: BuildStepContext, from: Iterable<Artifact>, to: Iterable<Artifact>, useLibs: Iterable<Artifact>): BuildResult = null!!

    internal fun compileKotlin(context: BuildStepContext, from: Iterable<Artifact>, to: Iterable<Artifact>, useLibs: Iterable<Artifact>, rule: KotlinCompilerRule): BuildResult {
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

        args.freeArgs = from.getAllStreams().map { it.path.toString() }
        if (args.freeArgs.size() == 0) {
            messageCollector.report(
                    CompilerMessageSeverity.ERROR,
                    "No souurces to compile in module ${project.moduleName}: $from",
                    CompilerMessageLocation.NO_LOCATION)
            return BuildResult.Fail
        }

        messageCollector.report(
                CompilerMessageSeverity.INFO,
                "compiling module ${project.moduleName} from $from to $to",
                CompilerMessageLocation.NO_LOCATION)

        val libraries = useLibs
                .flatMap { when (it) {
                        is komplex.FileArtifact -> listOf(it.path)
                        is komplex.LibraryWithDependenciesArtifact -> it.resolvedPaths
                        else -> throw Exception("unsupported lib type: $it")
                } }
                .map { it.toAbsolutePath().toString() }
                .toSortedSet()
                .joinToString(File.pathSeparator)

        //log.debug("classpath: {}", libraries)
        messageCollector.report(CompilerMessageSeverity.INFO, "build classpath: $libraries", CompilerMessageLocation.NO_LOCATION)

        args.classpath = libraries

        val folder = to.single()
        when (folder) {
            is FolderArtifact -> args.destination = folder.path.toString()
            else -> throw IllegalArgumentException("Compiler only supports single folder as destination")
        }

        val exitCode = compiler.exec(messageCollector, Services.EMPTY, args)

        messageCollector.report(
                CompilerMessageSeverity.INFO,
                "compiling result: $exitCode for module ${project.moduleName}",
                CompilerMessageLocation.NO_LOCATION)

        return when (exitCode) {
            ExitCode.OK -> BuildResult.Success
            else -> BuildResult.Fail
        }
    }
}
