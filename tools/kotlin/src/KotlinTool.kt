package komplex.kotlin

import java.lang
import java.io.*
import java.util.HashMap
import java.nio.file.Paths
import java.nio.file.Path
import org.jetbrains.jet.cli.common.messages.MessageCollector
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation
import org.jetbrains.jet.cli.common.ExitCode
import org.jetbrains.jet.cli.jvm.K2JVMCompiler
import org.jetbrains.jet.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.jet.config.Services
import kotlin.properties.Delegates
import komplex.model
import komplex.dsl
import komplex.tools
import komplex.data
import komplex.utils


public val dsl.tools.kotlin: KotlinCompilerRule
    get() = KotlinCompilerRule(komplex.model.LazyTool<KotlinCompilerRule, KotlinCompiler>("Kotlin compiler", { KotlinCompiler()} ))


// separate class for separate class loading
// \todo check if moving to separate file or jar is needed for really lazy tool loading, or may be that nested class will work as well
public class KotlinCompilerRule(kotlinCompiler: komplex.model.Tool<KotlinCompilerRule>) : komplex.tools.CompilerRule() {
    override val name: String = "Kotlin compiler"

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

        val explicitSourcesSet = cfg.explicitSources.toHashSet()

        args.freeArgs = src.filter { explicitSourcesSet.contains(it.first) }
                           .flatMap { data.openFileSet(it).coll.map { it.path.toString() }}
        if (args.freeArgs.size() == 0) {
            messageCollector.report(
                    CompilerMessageSeverity.ERROR,
                    "No souurces to compile in module ${project.name}: ${src.map { it.first }}",
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
