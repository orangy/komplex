
package komplex.tools

import java.util.ArrayList
import komplex.model.targets
import komplex.dsl.RuleImpl
import komplex.model.ArtifactDesc
import komplex.dsl.Rule
import komplex.dsl.Module
import komplex.dsl.ModuleDependency
import java.util.concurrent.CopyOnWriteArrayList

// \todo validate each use on addition

public abstract class CompilerRule : RuleImpl() {
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

public fun CompilerRule.use(vararg artifacts: ArtifactDesc): CompilerRule {
    artifacts.forEach { usedLibs.add(it) }
    return this
}

public fun CompilerRule.use(vararg rules: Rule): CompilerRule {
    rules.forEach { usedRules.add(it) }
    return this
}

public fun CompilerRule.use(vararg modules: Module): CompilerRule {
    modules.forEach { usedModules.add(it) }
    return this
}

public fun CompilerRule.use(vararg moduleDeps: ModuleDependency): CompilerRule {
    moduleDeps.forEach { explicitDependencies.add(it) }
    return this
}

public fun CompilerRule.use(vararg deps: Iterable<*>): CompilerRule {
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
