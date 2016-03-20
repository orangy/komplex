
package komplex.tools.proguard

import komplex.dsl.FileArtifact
import komplex.dsl.FileGlobArtifact
import komplex.dsl.artifacts
import komplex.model.ArtifactData
import komplex.model.ArtifactDesc
import komplex.model.BuildContext
import komplex.model.BuildResult
import komplex.tools.configureSingleFileTarget
import komplex.utils.BuildDiagnostic
import komplex.utils.escape4cli
import komplex.utils.plus
import komplex.utils.runProcess
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.text.RegexOption

val komplex.dsl.tools.proguard: ProGuardRule get() = ProGuardRule(ProGuardTool())

val log = LoggerFactory.getLogger("komplex.tools.proguard")


// separate class for separate class loading
class ProGuardRule(proGuardTool: ProGuardTool) : komplex.dsl.BasicToolRule<ProGuardRule, komplex.model.Tool<ProGuardRule>>(proGuardTool) {
    // configuration params
    // note: filters are applied to all inputs (injars) for now
    // \todo implement flexible scheme of adding filters to particular inputs, e.g. using specific "from" implementations
    val filters: MutableList<() -> String> = arrayListOf()
    val options: MutableList<() -> String> = arrayListOf()

    override fun configure(): BuildDiagnostic =
            super.configure() +
                    configureSingleFileTarget(module, artifacts.jar, { "${module.name}.jar" })
}

fun ProGuardRule.filters(vararg flt: String): ProGuardRule {
    flt.forEach { filters.add {it} }
    return this
}

fun ProGuardRule.filters(vararg flt: () -> String): ProGuardRule {
    filters.addAll(flt)
    return this
}

fun ProGuardRule.options(vararg opt: String): ProGuardRule {
    opt.forEach { options.add {it} }
    return this
}
fun ProGuardRule.options(vararg opt: () -> String): ProGuardRule {
    options.addAll(opt)
    return this
}

// compresses all sources into single destination described by the first target
// \todo add multiple targets and append support
class ProGuardTool : komplex.model.Tool<ProGuardRule> {
    override val name: String = "ProGuard tool"

    override fun execute(context: BuildContext, cfg: ProGuardRule, src: Iterable<Pair<ArtifactDesc, ArtifactData?>>, tgt: Iterable<ArtifactDesc>): BuildResult {

        val targetDesc = tgt.single()
        val targetPath =
                when {
                    targetDesc is FileArtifact -> targetDesc.path
                    else -> throw IllegalArgumentException("$targetDesc is not supported in $name")
                }

        log.info("$name target: ${targetPath}")

        val sourcePaths = arrayListOf<Path>()

        for (sourcePair in src) {
            val sourceDesc = sourcePair.first
            when {
                sourceDesc is FileGlobArtifact && sourceDesc.type == komplex.dsl.artifacts.jar -> sourcePaths.add(sourceDesc.path)
                sourceDesc is FileArtifact && sourceDesc.type == komplex.dsl.artifacts.jar -> sourcePaths.add(sourceDesc.path)
                else -> throw IllegalArgumentException("$sourceDesc is not supported in $name")
            }
        }

        if (sourcePaths.none())
            throw IllegalArgumentException("no sources given for $name")

        // compute it once
        val filters = cfg.filters.map { it() }
        val filteredSrc = fun (src: Path) = "${escape4cli(src.toString())}" +
                if (filters.any()) filters.map { escape4cli(it) }.joinToString(",","(",")") else ""

        // compute it once
        val options = cfg.options.map { it() }
        options.forEach {
            if ("(in|out)jars".toRegex(RegexOption.IGNORE_CASE).matches(it))
                log.warn("using -injars or -outjars in directly in options could lead to undesirable results, use from/into/export outside of options instead")
        }

        val pgcmdline = arrayListOf(
                "java",
                "-jar",
                Paths.get(this.javaClass.protectionDomain.codeSource.location.toURI().path).toAbsolutePath().resolve("../../../lib/proguard-base.jar").toString()) +
                sourcePaths.flatMap{ listOf("-injars", filteredSrc(it)) } +
                "-outjars" +
                "${escape4cli(targetPath.toString())}" +
                options.flatMap { it.split("[\\r\\n]+".toRegex()).map { it.replace("#.*$","") }.flatMap { it.split("\\s+".toRegex()) } }

        log.debug("proguard params: ${pgcmdline.joinToString("\n  ","\n  ")}")

        val res = runProcess(pgcmdline, { log.info(it) }, { log.error(it) })
        return if (res == 0) BuildResult(BuildDiagnostic.Success, listOf(Pair(targetDesc, null)))
            else BuildResult(BuildDiagnostic.Fail)
    }
}
