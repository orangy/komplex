package komplex.tools.kotlin

import komplex.data.openFileSet
import komplex.dsl.*
import komplex.model.ArtifactDesc
import komplex.model.Tool
import komplex.tools.filterIn
import komplex.tools.getPaths
import komplex.utils.BuildDiagnostic
import komplex.utils.escape4cli
import komplex.utils.plus
import komplex.utils.runProcess
import java.nio.file.Path


class KotlinJSRule(tool: Tool<KotlinJSRule>) : komplex.dsl.BasicToolRule<KotlinJSRule, komplex.model.Tool<KotlinJSRule>>(tool) {

    val explicitMetaTargets: MutableCollection<Artifact> = arrayListOf()
    override val targets: Iterable<ArtifactDesc> get() = explicitTargets + explicitMetaTargets
    var includeStdlib: Boolean = true
    var main: String = ""

    override fun configure(): BuildDiagnostic {
        var ret = super.configure()
        if (explicitTargets.none() || explicitMetaTargets.none()) {
            val tempDir = module.env.tempDir
            if (tempDir != null) {
                val stepTempDir = tempDir / (module.name + "." + name.replace(' ', '_'))
                if (explicitTargets.none()) into(module.file(artifacts.source, stepTempDir / "out.js"))
                if (explicitMetaTargets.none()) into(module.file(artifacts.source, stepTempDir / "meta.js"))
            }
            else ret = ret + BuildDiagnostic.Fail("$name (${module.fullName}) Cannot auto configure targets: tempDir is not defined")
        }
        return ret
    }
}

fun KotlinJSRule.meta(vararg artifacts: Iterable<Artifact>): KotlinJSRule {
    artifacts.forEach { explicitMetaTargets.addAll(it) }
    return this
}

fun KotlinJSRule.meta(vararg artifacts: Artifact): KotlinJSRule {
    explicitMetaTargets.addAll(artifacts)
    return this
}


fun komplex.dsl.tools.kotlinjs(compilerCmd: Iterable<String>): KotlinJSRule =
        KotlinJSRule(komplex.model.LazyTool<KotlinJSRule, KotlinExternalJSCompiler>("Kotlin to Javascript compiler", { KotlinExternalJSCompiler(compilerCmd) } ))

fun komplex.dsl.tools.kotlinjs(vararg compilerCmd: String): KotlinJSRule = komplex.dsl.tools.kotlinjs(compilerCmd.asIterable())

fun komplex.dsl.tools.kotlinjs(compilerJarPath: Path): KotlinJSRule = komplex.dsl.tools.kotlinjs("java", "-cp", compilerJarPath.toString(), "org.jetbrains.kotlin.cli.js.K2JSCompiler")


class KotlinExternalJSCompiler(val compilerCmd: Iterable<String>) : komplex.model.Tool<KotlinJSRule> {

    override val name: String = "Kotlin external JS compiler"

    override fun execute(context: komplex.model.BuildContext,
                         cfg: KotlinJSRule,
                         src: Iterable<Pair<komplex.model.ArtifactDesc, komplex.model.ArtifactData?>>,
                         tgt: Iterable<komplex.model.ArtifactDesc>
    ): komplex.model.BuildResult {

        val project = context.module

        val kotlinSources = src.filterIn(cfg.fromSources).getPaths()

        if (kotlinSources.none()) {
            log.error("Error: No sources to compile in module ${project.name}: ${src.map { it.first }}")
            return komplex.model.BuildResult(komplex.utils.BuildDiagnostic.Fail)
        }

        // first is target second is meta
        val dests = arrayOf(tgt.filter { it in cfg.explicitTargets }.first() as FileArtifact,
                tgt.filter { it in cfg.explicitMetaTargets }.first() as FileArtifact)
        // \todo warnings in case of ignored targets

        dests.map { it.path.parent.toFile() }.forEach { if (!it.exists()) it.mkdirs() }

        val ktccmdline: MutableList<String> = compilerCmd.map { escape4cli(it) }.toMutableList()

        ktccmdline.addAll(listOf("-output", escape4cli(dests[0].path.toString()),
                                 "-meta-info", escape4cli(dests[1].path.toString()),
                                 "-main", if (cfg.main.isBlank()) "noCall" else escape4cli(cfg.main)))

        if (!cfg.includeStdlib) ktccmdline.add("-no-stdlib")

        ktccmdline.addAll(kotlinSources.map { escape4cli(it.toString()) })

        val res = runProcess(ktccmdline, { log.info(it) }, { log.info(it) })

        return if (res == 0) komplex.model.BuildResult(komplex.utils.BuildDiagnostic.Success, dests.map{ Pair(it, openFileSet(it)) })
        else komplex.model.BuildResult(komplex.utils.BuildDiagnostic.Fail)
    }
}

