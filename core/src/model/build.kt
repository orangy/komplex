
package komplex.model

import komplex.data.hashEquals
import komplex.data.mergeHashes
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
                            targets: Iterable<BuildGraphNode> = this.leafs(scenario))
{
    buildPartialApply(
           scenario,
           { n ->
               //log.trace()
               val hash = mergeHashes(n.step.sources.map { context.artifacts.get(it) })
               if (n.step.targets.all { context.artifacts.get(it)?.sourcesHash?.hashEquals(hash) ?: false }) {
                   log.debug("skipping: already built: ${n.step.name} in module ${n.moduleFlavor.module.name}")
                   false
               }
               else {
                   context.node = n
                   val artifacts = hashMapOf<ArtifactDesc, ArtifactData?>()
                   n.step.sources.forEach { artifacts.set(it, context.artifacts.get(it)) }
                   log.debug("running ${n.step.name} in module ${n.moduleFlavor.module.name}")
                   val result = n.step.execute(context, artifacts)
                   result.result.forEach { context.artifacts.set(it.first, it.second) }
                   log.info("${n.step.name} ${if (result.diagnostic != BuildDiagnostic.Success) "failed: ${result.diagnostic.message}" else "succeeded!"}")
                   result.diagnostic != BuildDiagnostic.Success
               }
           },
           sources,
           targets)
}


