package komplex.tools.kotlin

import com.sun.tools.javac.resources.compiler
import komplex.dsl.Artifact
import komplex.dsl.FileArtifact
import komplex.dsl.FolderArtifact
import komplex.dsl.RuleImpl
import komplex.model
import komplex.model.*
import komplex.tools.filterIn
import komplex.tools.getPaths
import komplex.utils
import komplex.tools.kotlin.KotlinCompiler
import komplex.tools.kotlin.KotlinCompilerRule
import komplex.tools.kotlin.log
import komplex.utils.escape4cli
import komplex.utils.runProcess
import komplex.data.openFileSet
import java.io.File
import java.nio.file.Path
import java.util.*


public class KotlinJSRule(tool: Tool<KotlinJSRule>) : komplex.dsl.BasicToolRule<KotlinJSRule, komplex.model.Tool<KotlinJSRule>>(tool) {

    public val explicitMetaTargets: MutableCollection<Artifact> = arrayListOf()
    override val targets: Iterable<ArtifactDesc> get() = explicitTargets + explicitMetaTargets
    public var includeStdlib: Boolean = true
    public var main: String = ""
}

public fun KotlinJSRule.meta(vararg artifacts: Iterable<Artifact>): KotlinJSRule {
    artifacts.forEach { explicitMetaTargets.addAll(it) }
    return this
}

public fun KotlinJSRule.meta(vararg artifacts: Artifact): KotlinJSRule {
    explicitMetaTargets.addAll(artifacts)
    return this
}


public fun komplex.dsl.tools.kotlinjs(compilerCmd: Iterable<String>): KotlinJSRule =
        KotlinJSRule(komplex.model.LazyTool<KotlinJSRule, KotlinExternalJSCompiler>("Kotlin to Javascript compiler", { KotlinExternalJSCompiler(compilerCmd) } ))

public fun komplex.dsl.tools.kotlinjs(vararg compilerCmd: String): KotlinJSRule = komplex.dsl.tools.kotlinjs(compilerCmd.asIterable())

public fun komplex.dsl.tools.kotlinjs(compilerJarPath: Path): KotlinJSRule = komplex.dsl.tools.kotlinjs("java", "-cp", compilerJarPath.toString(), "org.jetbrains.kotlin.cli.js.K2JSCompiler")


public class KotlinExternalJSCompiler(val compilerCmd: Iterable<String>) : komplex.model.Tool<KotlinJSRule> {

    override val name: String = "Kotlin external JS compiler"

    override fun execute(context: model.BuildContext,
                         cfg: KotlinJSRule,
                         src: Iterable<Pair<model.ArtifactDesc, model.ArtifactData?>>,
                         tgt: Iterable<model.ArtifactDesc>
    ): model.BuildResult {

        val project = context.module

        val kotlinSources = src.filterIn(cfg.fromSources).getPaths()

        if (kotlinSources.none()) {
            log.error("Error: No sources to compile in module ${project.name}: ${src.map { it.first }}")
            return model.BuildResult(utils.BuildDiagnostic.Fail)
        }

        // first is target second is meta
        val dests = arrayOf(tgt.filter { it in cfg.explicitTargets }.first() as FileArtifact,
                tgt.filter { it in cfg.explicitMetaTargets }.first() as FileArtifact)
        // \todo warnings in case of ignored targets

        dests.map { it.path.getParent().toFile() }.forEach { if (!it.exists()) it.mkdirs() }

        val ktccmdline: ArrayList<String> = compilerCmd.map { escape4cli(it) }.toArrayList()

        ktccmdline.addAll(listOf("-output", escape4cli(dests[0].path.toString()),
                                 "-meta-info", escape4cli(dests[1].path.toString()),
                                 "-main", if (cfg.main.isBlank()) "noCall" else escape4cli(cfg.main)))

        if (!cfg.includeStdlib) ktccmdline.add("-no-stdlib")

        ktccmdline.addAll(kotlinSources.map { escape4cli(it.toString()) })

        val res = runProcess(ktccmdline, { log.info(it) }, { log.info(it) })

        return if (res == 0) model.BuildResult(utils.BuildDiagnostic.Success, dests.map{ Pair(it, openFileSet(it)) })
        else model.BuildResult(utils.BuildDiagnostic.Fail)
    }
}

