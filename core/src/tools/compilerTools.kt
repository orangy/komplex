
package komplex.tools

import komplex.data.OpenFileSet
import komplex.data.openFileSet
import komplex.dsl.*
import komplex.dsl.Module
import komplex.dsl.ModuleDependency
import komplex.model.*
import komplex.utils.BuildDiagnostic
import komplex.utils.plus
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.platform.platformName

// \todo validate each use on addition

public abstract class RuleWithClasspathImpl : RuleImpl() {

    public val explicitClasspath: RuleSources = RuleSources()

    public val classpathSources: Iterable<ArtifactDesc> get() = explicitClasspath.collect(selector.scenarios)

    override val sources: Iterable<ArtifactDesc> get() = super<RuleImpl>.sources + classpathSources
}

public fun <T : RuleWithClasspathImpl, S: GenericSourceType> T.classpath(args: Iterable<S>): T = addToSources(explicitClasspath, args)
public fun <T : RuleWithClasspathImpl, S: GenericSourceType> T.classpath(vararg args: S): T = addToSources(explicitClasspath, *args)


public abstract class JVMCompilerRule<Config: JVMCompilerRule<Config, T>, T: Tool<Config>> : RuleWithClasspathImpl(), ToolStep<Config, T> {

    override fun configure(module: komplex.model.Module, scenarios: Scenarios): BuildDiagnostic =
            super<RuleWithClasspathImpl>.configure(module, scenarios) +
                    // \todo generic and robust conversion to file-name friendly string, also in other places
                    configureSingleTempFolderTarget(module as komplex.dsl.Module, artifacts.binaries, { "${module.name}.${name.replace(' ', '_')}" })
}

platformName("getPaths_Pairs_of_ArtifactDesc_ArtifactData")
public fun Iterable<Pair<ArtifactDesc, ArtifactData?>>.getPaths(options: OpenFileSet = OpenFileSet.Nothing): Iterable<Path> =
        this.flatMap { openFileSet(it, options = options).coll.map { it.path.toAbsolutePath().normalize() } }
        // \todo consider converting to relative/optimal paths

platformName("getPaths_ArtifactDescs")
public fun Iterable<ArtifactDesc>.getPaths(options: OpenFileSet = OpenFileSet.Nothing): Iterable<Path> =
        this.flatMap { openFileSet(it, options = options).coll.map { it.path.toAbsolutePath().normalize() } }


public fun Iterable<Pair<ArtifactDesc, ArtifactData?>>.filterIn(explicitSourcesSet: Set<ArtifactDesc>): Iterable<Pair<ArtifactDesc, ArtifactData?>> =
        this.filter { explicitSourcesSet.contains(it.first) }

public fun Iterable<Pair<ArtifactDesc, ArtifactData?>>.filterIn(explicitSources: Iterable<ArtifactDesc>): Iterable<Pair<ArtifactDesc, ArtifactData?>> =
        this.filterIn(explicitSources.toHashSet())


public fun Iterable<ArtifactDesc>.singleDestFolder(): FolderArtifact =
        this.single() as? FolderArtifact ?:
                throw IllegalArgumentException("Compiler only supports single folder as destination")
