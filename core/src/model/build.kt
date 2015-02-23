
package komplex.model

import komplex.utils.BuildDiagnostic


public open class GraphBuildContext(override val scenario: Scenarios, public val graph: BuildGraph) : BuildContext {
    public var node: BuildGraphNode? = null
    override val module: Module
        get() = node!!.moduleFlavor.module
    public val artifacts: MutableMap<ArtifactDesc, ArtifactData?> = hashMapOf()
}


public fun BuildGraph.build(scenario: Scenarios, context: GraphBuildContext = GraphBuildContext(scenario, this)) {
    buildAllApply( scenario,
                   { (n) ->
                       context.node = n
                       val artifacts = hashMapOf<ArtifactDesc, ArtifactData?>()
                       n.step.sources.forEach { artifacts.set(it, context.artifacts.get(it)) }
                       val result = n.step.execute(context, artifacts)
                       result.result.forEach { context.artifacts.set(it.first, it.second) }
                       result.diagnostic != BuildDiagnostic.Success
                   })
}

