
package komplex.tools

import komplex.data.OpenFileSet
import komplex.data.openFileSet
import komplex.dsl.*
import komplex.model.ArtifactData
import komplex.model.ArtifactDesc
import komplex.model.Tool
import komplex.model.ToolStep
import komplex.utils.BuildDiagnostic
import komplex.utils.plus
import java.nio.file.Path

// \todo validate each use on addition

abstract class RuleWithClasspathImpl : RuleImpl() {
    val explicitClasspath: RuleSources = RuleSources()

    val classpathSources: Iterable<ArtifactDesc> get() = explicitClasspath.collect(selector.scenarios)

    override val sources: Iterable<ArtifactDesc> get() = super.sources + classpathSources
}

fun <T : RuleWithClasspathImpl, S: GenericSourceType> T.classpath(args: Iterable<S>): T = addToSources(explicitClasspath, args)
fun <T : RuleWithClasspathImpl, S: GenericSourceType> T.classpath(vararg args: S): T = addToSources(explicitClasspath, *args)


abstract class JVMCompilerRule<Config: JVMCompilerRule<Config, T>, T: Tool<Config>> : RuleWithClasspathImpl(), ToolStep<Config, T> {

    override fun configure(): BuildDiagnostic =
            super.configure() +
                    // \todo generic and robust conversion to file-name friendly string, also in other places
                    configureSingleTempFolderTarget(module, artifacts.binary, { "${module.name}.${name.replace(' ', '_')}" })
}

@JvmName("getPaths_Pairs_of_ArtifactDesc_ArtifactData")
fun Iterable<Pair<ArtifactDesc, ArtifactData?>>.getPaths(options: OpenFileSet = OpenFileSet.Nothing): Iterable<Path> =
        this.flatMap { openFileSet(it, options = options).coll.map { it.path.toAbsolutePath().normalize() } }
        // \todo consider converting to relative/optimal paths

@JvmName("getPaths_ArtifactDescs")
fun Iterable<ArtifactDesc>.getPaths(options: OpenFileSet = OpenFileSet.Nothing): Iterable<Path> =
        this.flatMap { openFileSet(it, options = options).coll.map { it.path.toAbsolutePath().normalize() } }


fun Iterable<Pair<ArtifactDesc, ArtifactData?>>.filterIn(explicitSourcesSet: Set<ArtifactDesc>): Iterable<Pair<ArtifactDesc, ArtifactData?>> =
        this.filter { explicitSourcesSet.contains(it.first) }

fun Iterable<Pair<ArtifactDesc, ArtifactData?>>.filterIn(explicitSources: Iterable<ArtifactDesc>): Iterable<Pair<ArtifactDesc, ArtifactData?>> =
        this.filterIn(explicitSources.toHashSet())


fun Iterable<ArtifactDesc>.singleDestFolder(): FolderArtifact =
        this.single() as? FolderArtifact ?:
                throw IllegalArgumentException("Compiler only supports single folder as destination")
