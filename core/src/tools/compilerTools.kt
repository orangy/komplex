
package komplex.tools

import java.util.ArrayList
import komplex.model.targets
import komplex.dsl.RuleImpl
import komplex.model.ArtifactDesc
import komplex.dsl.Rule
import komplex.dsl.Module
import komplex.dsl.ModuleDependency
import komplex.model.Tool
import komplex.model.ToolStep
import komplex.utils.BuildDiagnostic
import java.util.concurrent.CopyOnWriteArrayList

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
