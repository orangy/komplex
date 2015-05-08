
package komplex.tools

import komplex.data.OpenFileSet
import komplex.data.openFileSet
import komplex.dsl.*
import komplex.dsl.Module
import komplex.dsl.ModuleDependency
import komplex.model.*
import komplex.utils.BuildDiagnostic
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.platform.platformName

// \todo validate each use on addition

public abstract class CompilerRule<Config: CompilerRule<Config, T>, T: Tool<Config>> : RuleImpl(), ToolStep<Config, T> {
    override val sources: Iterable<ArtifactDesc> get() {
        usedSources.forEach { use(it) }
        usedSources.clear()
        return explicitSources + depSources
    }
    override val depSources: Iterable<ArtifactDesc> get() =
        dependsOn.flatMap { it.targets(selector.scenarios) } + usedLibs + usedRules.flatMap { it.targets }

    public val usedLibs: MutableCollection<ArtifactDesc> = arrayListOf()
    public val usedRules: MutableCollection<Rule> = arrayListOf()
    public val usedModules: MutableCollection<Module> = arrayListOf()
    public val usedSources: MutableCollection<Iterable<*>> = CopyOnWriteArrayList() // todo: find out why concurrent collection is needed
}

public fun<Config: CompilerRule<Config, T>, T: Tool<Config>> CompilerRule<Config, T>.use(vararg artifacts: ArtifactDesc): CompilerRule<Config, T> {
    artifacts.forEach { usedLibs.add(it) }
    return this
}

public fun<Config: CompilerRule<Config, T>, T: Tool<Config>> CompilerRule<Config, T>.use(vararg artifactssets: ArtifactsSet): CompilerRule<Config, T> {
    artifactssets.forEach { it.members.forEach { usedLibs.add(it) } }
    return this
}

public fun<Config: CompilerRule<Config, T>, T: Tool<Config>> CompilerRule<Config, T>.use(vararg rules: Rule): CompilerRule<Config, T> {
    rules.forEach { usedRules.add(it) }
    return this
}

public fun<Config: CompilerRule<Config, T>, T: Tool<Config>> CompilerRule<Config, T>.use(vararg modules: Module): CompilerRule<Config, T> {
    modules.forEach { usedModules.add(it) }
    return this
}

public fun<Config: CompilerRule<Config, T>, T: Tool<Config>> CompilerRule<Config, T>.use(vararg moduleDeps: ModuleDependency): CompilerRule<Config, T> {
    moduleDeps.forEach { explicitDependencies.add(it) }
    return this
}

public fun<Config: CompilerRule<Config, T>, T: Tool<Config>> CompilerRule<Config, T>.use(vararg deps: Iterable<*>): CompilerRule<Config, T> {
    for (dep in deps) {
        var sample = dep.firstOrNull()
        when (sample) {
            is ArtifactDesc -> dep.forEach { use(it as ArtifactDesc) }
            is Rule -> dep.forEach { use(it as Rule) }
            is Module -> dep.forEach { use(it as Module) }
            is ModuleDependency -> dep.forEach { use(it as ModuleDependency) }
            // \todo consider using only the next variant on building
            null -> usedSources.add(dep)
            else -> throw Exception("Unknown type for use construct: $sample")
        }
    }
    return this
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
