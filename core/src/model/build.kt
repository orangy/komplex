
package komplex.model

import komplex.utils.BuildDiagnostic


public open class GraphBuildContext(val baseScenario: Scenarios, public val graph: BuildGraph) : BuildContext {
    public var node: BuildGraphNode? = null
    override val module: Module
        get() = node!!.moduleFlavor.module
    override val scenario = node?.moduleFlavor?.scenarios?.resolve(baseScenario) ?: baseScenario
    public val artifacts: MutableMap<ArtifactDesc, ArtifactData?> = hashMapOf()
}


public fun BuildGraph.build(scenario: Scenarios,
                            context: GraphBuildContext = GraphBuildContext(scenario, this),
                            sources: Set<BuildGraphNode> = this.roots(scenario).toHashSet(),
                            targets: Iterable<BuildGraphNode> = this.leafs(scenario)) {
    buildPartialApply(
           scenario,
           { (n) ->
               context.node = n
               val artifacts = hashMapOf<ArtifactDesc, ArtifactData?>()
               n.step.sources.forEach { artifacts.set(it, context.artifacts.get(it)) }
               println("[BUILD] running ${n.step.name} in module ${n.moduleFlavor.module.name}")
               val result = n.step.execute(context, artifacts)
               result.result.forEach { context.artifacts.set(it.first, it.second) }
               println("[BUILD] ${n.step.name} ${if (result.diagnostic != BuildDiagnostic.Success) "failed: ${result.diagnostic.message}" else "succeeded!"}")
               result.diagnostic != BuildDiagnostic.Success
           },
           sources,
           targets)
}

/*
public fun BuildGraph.build( scenario: Scenarios,
                             sources: Set<BuildGraphNode> = this.roots(scenario).toHashSet(),
                             targets: Iterable<BuildGraphNode> = this.leafs(scenario)) {
    val artifacts: MutableMap<ArtifactDesc, ArtifactData?> = hashMapOf()
    buildPartialApply(
            scenario,
            { (n) ->
                val context = object : BuildContext {
                    override val scenario = n.moduleFlavor.scenarios.resolve(scenario)
                    override val module = n.moduleFlavor.module
                }
                val res = n.step.execute(context, artifacts)
                if (res.diagnostic == BuildDiagnostic.Fail)
                    false
                else {
                    res.result.forEach { artifacts.put(it.first, it.second) }
                    true
                }
            },
            sources,
            targets)
}
*/
