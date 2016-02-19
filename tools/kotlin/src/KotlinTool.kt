package komplex.tools.kotlin

import komplex.data.OpenFileSet
import komplex.dsl.FolderArtifact
import komplex.model.Tool
import komplex.tools.filterIn
import komplex.tools.getPaths
import komplex.tools.singleDestFolder
import komplex.utils.BuildDiagnostic
import org.slf4j.LoggerFactory
import java.nio.file.Path


internal val log = LoggerFactory.getLogger("komplex.tools.kotlin")

class KotlinCompilerRule(override val tool: Tool<KotlinCompilerRule>) : komplex.tools.JVMCompilerRule<KotlinCompilerRule, Tool<KotlinCompilerRule>>() {
    override val name: String = "Kotlin compiler"
    override val config: KotlinCompilerRule = this

    fun invoke(body: KotlinCompilerRule.() -> Unit): KotlinCompilerRule {
        body()
        return this
    }
    var enableInline: Boolean = true
    var includeRuntime: Boolean = true
    val sourceRoots: MutableCollection<String> = arrayListOf()
}


abstract class KotlinCompiler() : komplex.model.Tool<KotlinCompilerRule> {

    override fun execute(context: komplex.model.BuildContext,
                         cfg: KotlinCompilerRule,
                         src: Iterable<Pair<komplex.model.ArtifactDesc, komplex.model.ArtifactData?>>,
                         tgt: Iterable<komplex.model.ArtifactDesc>
    ): komplex.model.BuildResult {

        val project = context.module

        val destFolder = tgt.singleDestFolder()
        val kotlinSources = src.filterIn(cfg.fromSources).getPaths()

        if (kotlinSources.none()) {
            log.error("Error: No sources to compile in module ${project.name}: ${src.map { it.first }}")
            return komplex.model.BuildResult(komplex.utils.BuildDiagnostic.Fail)
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
            komplex.utils.BuildDiagnostic.Status.Succeeded -> komplex.model.BuildResult(exitCode, listOf(Pair(destFolder, komplex.data.openFileSet(destFolder))))
            else -> komplex.model.BuildResult(exitCode)
        }
    }

    protected abstract fun compile( destFolder: FolderArtifact,
                                    kotlinSources: Iterable<Path>,
                                    sourceRoots: Iterable<String>,
                                    libraries: Iterable<Path>,
                                    includeRuntime: Boolean)
            : BuildDiagnostic
}

