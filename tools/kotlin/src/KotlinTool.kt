package komplex.tools.kotlin

import java.lang
import java.io.*
import java.util.HashMap
import java.nio.file.Paths
import java.nio.file.Path
import org.slf4j.LoggerFactory
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.Services
import kotlin.properties.Delegates
import komplex.model
import komplex.dsl
import komplex.tools
import komplex.data
import komplex.model.Tool
import komplex.model.ToolStep
import komplex.utils


public val komplex.dsl.tools.kotlin: KotlinCompilerRule
    get() = KotlinCompilerRule(komplex.model.LazyTool<KotlinCompilerRule, KotlinCompiler>("Kotlin compiler", { KotlinCompiler()} ))

val log = LoggerFactory.getLogger("komplex.tools.kotlin")

// separate class for separate class loading
// \todo check if moving to separate file or jar is needed for really lazy tool loading, or may be that nested class will work as well
public class KotlinCompilerRule(override val tool: Tool<KotlinCompilerRule>) : komplex.tools.CompilerRule<KotlinCompilerRule, Tool<KotlinCompilerRule>>() {
    override val name: String = "Kotlin compiler"
    override val config: KotlinCompilerRule = this

    public fun invoke(body: KotlinCompilerRule.() -> Unit): KotlinCompilerRule {
        body()
        return this
    }
    public var enableInline: Boolean = true
}


public class KotlinCompiler() : komplex.model.Tool<KotlinCompilerRule> {
    override val name: String = "Kotlin compiler"

    //    override fun compile(context: BuildStepContext, from: Iterable<Artifact>, to: Iterable<Artifact>, useLibs: Iterable<Artifact>): BuildResult = null!!

    override fun execute(context: model.BuildContext,
                         cfg: KotlinCompilerRule,
                         src: Iterable<Pair<model.ArtifactDesc, model.ArtifactData?>>,
                         tgt: Iterable<model.ArtifactDesc>
    ): model.BuildResult {
        val compiler = K2JVMCompiler()
        val args = K2JVMCompilerArguments()
        val messageCollector = object : MessageCollector {
            override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation) {
                fun msg() = if (location == CompilerMessageLocation.NO_LOCATION) "$message" else "$message ($location)"
                when (severity) {
                    in CompilerMessageSeverity.ERRORS -> log.error(msg())
                    CompilerMessageSeverity.LOGGING -> log.debug(msg())
                    CompilerMessageSeverity.INFO -> log.info(msg())
                    CompilerMessageSeverity.WARNING -> log.info(msg())
                    //CompilerMessageSeverity.OUTPUT -> log.trace(msg())
                }
            }
        }
        val project = context.module

        val explicitSourcesSet = cfg.explicitSources.toHashSet()

        args.freeArgs = src.filter { explicitSourcesSet.contains(it.first) }
                           .flatMap { data.openFileSet(it).coll.map { it.path.toString() }}
        if (args.freeArgs.size() == 0) {
            messageCollector.report(
                    CompilerMessageSeverity.ERROR,
                    "No sources to compile in module ${project.name}: ${src.map { it.first }}",
                    CompilerMessageLocation.NO_LOCATION)
            return model.BuildResult(utils.BuildDiagnostic.Fail)
        }

        messageCollector.report(
                CompilerMessageSeverity.INFO,
                "compiling module ${project.name} from ${args.freeArgs} to $tgt",
                CompilerMessageLocation.NO_LOCATION)

        val dependenciesSet = cfg.depSources.toHashSet()

        val libraries = src
                .filter { dependenciesSet.contains(it.first) }
                .flatMap { data.openFileSet(it).coll }
                .map { it.path.toAbsolutePath() }
                .distinct()
                // \todo convert to relative/optimal paths
                .joinToString(File.pathSeparator)

        //log.debug("classpath: {}", libraries)
        messageCollector.report(CompilerMessageSeverity.INFO, "build classpath: $libraries", CompilerMessageLocation.NO_LOCATION)

        args.classpath = libraries

        val folder = tgt.single()
        when (folder) {
            is dsl.FolderArtifact -> args.destination = folder.path.toString()
            else -> throw IllegalArgumentException("Compiler only supports single folder as destination")
        }

        log.info("kotlin: $args")

        val exitCode = compiler.exec(messageCollector, Services.EMPTY, args)
        // \todo extract actually compiled class files

        messageCollector.report(
                CompilerMessageSeverity.INFO,
                "compiling result: $exitCode for module ${project.name}",
                CompilerMessageLocation.NO_LOCATION)

        return when (exitCode) {
            ExitCode.OK -> model.BuildResult(utils.BuildDiagnostic.Success, listOf(Pair(folder, data.openFileSet(folder))))
            else -> model.BuildResult(utils.BuildDiagnostic.Fail)
        }
    }
}
