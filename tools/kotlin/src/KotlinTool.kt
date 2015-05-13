package komplex.tools.kotlin

import org.slf4j.LoggerFactory
import java.nio.file.Path
import komplex.data.openFileSet
import komplex.dsl.FolderArtifact
import komplex.data
import komplex.data.OpenFileSet
import komplex.model
import komplex.model.ArtifactData
import komplex.model.ArtifactDesc
import komplex.model.Tool
import komplex.tools.filterIn
import komplex.tools.getPaths
import komplex.tools.singleDestFolder
import komplex.utils
import komplex.utils.BuildDiagnostic


val log = LoggerFactory.getLogger("komplex.tools.kotlin")

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


public abstract class KotlinCompiler() : komplex.model.Tool<KotlinCompilerRule> {

    override fun execute(context: model.BuildContext,
                         cfg: KotlinCompilerRule,
                         src: Iterable<Pair<model.ArtifactDesc, model.ArtifactData?>>,
                         tgt: Iterable<model.ArtifactDesc>
    ): model.BuildResult {

        val project = context.module

        val destFolder = tgt.singleDestFolder()
        val kotlinSources = src.filterIn(cfg.fromSources).getPaths()

        if (kotlinSources.none()) {
            log.error("Error: No sources to compile in module ${project.name}: ${src.map { it.first }}")
            return model.BuildResult(utils.BuildDiagnostic.Fail)
        }

        val libraries = src.filterIn(cfg.classpathSources).getPaths(OpenFileSet.FoldersAsLibraries).distinct()

        log.info("compiling module ${project.name}")
        log.info("build sources: ${kotlinSources.joinToString("\n  ","\n  ")}")
        log.info("build target: $tgt")
        log.info("build classpath: ${libraries.joinToString("\n  ","\n  ")}")
        log.info("build java sources roots: ${cfg.sourceRoots.joinToString("\n  ","\n  ")}")

        val sourceRoots = cfg.sourceRoots
        val includeRuntime = cfg.includeRuntime

        val exitCode = compile(destFolder, kotlinSources, sourceRoots, libraries, includeRuntime)

        log.info("compiling result: $exitCode for module ${project.name}")

        return when (exitCode.status) {
            utils.BuildDiagnostic.Status.Succeeded -> model.BuildResult(exitCode, listOf(Pair(destFolder, data.openFileSet(destFolder))))
            else -> model.BuildResult(exitCode)
        }
    }

    protected abstract fun compile( destFolder: FolderArtifact,
                                    kotlinSources: Iterable<Path>,
                                    sourceRoots: Iterable<String>,
                                    libraries: Iterable<Path>,
                                    includeRuntime: Boolean)
            : BuildDiagnostic
}

