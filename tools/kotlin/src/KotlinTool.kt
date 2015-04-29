package komplex.tools.kotlin

import com.intellij.openapi.util.Disposer
import java.lang
import java.io.*
import java.util.HashMap
import java.nio.file.Paths
import java.nio.file.Path
import org.slf4j.LoggerFactory
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
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.addKotlinSourceRoot


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
    public var includeRuntime: Boolean = true
    public val sourceRoots: MutableCollection<String> = arrayListOf()
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
        val rootDisposable = Disposer.newDisposable()
        val compilerCfg = CompilerConfiguration()

//        val args = K2JVMCompilerArguments()

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

        val kotlinSources = src.filter { explicitSourcesSet.contains(it.first) }
                               .flatMap { data.openFileSet(it).coll.map { it.path.toString() }}

        if (kotlinSources.none()) {
            messageCollector.report(
                    CompilerMessageSeverity.ERROR,
                    "No sources to compile in module ${project.name}: ${src.map { it.first }}",
                    CompilerMessageLocation.NO_LOCATION)
            return model.BuildResult(utils.BuildDiagnostic.Fail)
        }

        kotlinSources.forEach { compilerCfg.addKotlinSourceRoot(it) }

        cfg.sourceRoots.forEach { compilerCfg.addJavaSourceRoot(File(it)) }

        val dependenciesSet = cfg.depSources.toHashSet()

        val libraries = src
                .filter { dependenciesSet.contains(it.first) }
                .flatMap { data.openFileSet(it).coll }
                .map { it.path.toAbsolutePath().normalize().toFile() }
                .distinct()
                .toList()
                // \todo convert to relative/optimal paths

        compilerCfg.addJvmClasspathRoots(libraries)

        compilerCfg.put<MessageCollector>(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)

        messageCollector.report(
                CompilerMessageSeverity.INFO,
                "compiling module ${project.name} from ${kotlinSources} to $tgt",
                CompilerMessageLocation.NO_LOCATION)
        messageCollector.report(CompilerMessageSeverity.INFO, "build classpath: $libraries", CompilerMessageLocation.NO_LOCATION)
        messageCollector.report(CompilerMessageSeverity.INFO, "build java sources roots: ${cfg.sourceRoots}", CompilerMessageLocation.NO_LOCATION)

        val destFolder = tgt.single() as? dsl.FolderArtifact ?:
                throw IllegalArgumentException("Compiler only supports single folder as destination")

        var exitCode = ExitCode.OK
        try {
            val environment = KotlinCoreEnvironment.createForProduction(rootDisposable, compilerCfg, EnvironmentConfigFiles.JVM_CONFIG_FILES)
            KotlinToJVMBytecodeCompiler.compileBunchOfSources(environment, null, destFolder.path.toFile(), cfg.includeRuntime)
        }
        catch (e: CompilationException) {
            messageCollector.report(CompilerMessageSeverity.EXCEPTION, OutputMessageUtil.renderException(e),
                    MessageUtil.psiElementToMessageLocation(e.getElement()))
            exitCode = ExitCode.INTERNAL_ERROR
        }

        // compiler.exec(messageCollector, Services.EMPTY, args)
        // \todo extract actually compiled class files

        messageCollector.report(
                CompilerMessageSeverity.INFO,
                "compiling result: $exitCode for module ${project.name}",
                CompilerMessageLocation.NO_LOCATION)

        return when (exitCode) {
            ExitCode.OK -> model.BuildResult(utils.BuildDiagnostic.Success, listOf(Pair(destFolder, data.openFileSet(destFolder))))
            else -> model.BuildResult(utils.BuildDiagnostic.Fail)
        }
    }
}
