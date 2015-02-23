
package komplex.model

import java.util.HashSet
import java.util.HashMap
import komplex.utils.subgraphDFS
import komplex.utils.makeVisitedTraversalChecker
import komplex.utils.graphDFS
import komplex.utils.IndentLn
import komplex.utils.BuildDiagnostic
import java.util.ArrayList

public data class ModuleFlavor(public val module: Module,
                               public val scenarios: Scenarios) {}

public data class BuildGraphNode(public val moduleFlavor: ModuleFlavor, public val step: Step) {}


public class BuildGraph() {

    val moduleSelectors: HashMap<ModuleFlavor, ScenarioSelector> = hashMapOf()
    // all nodes index
    val nodes: HashSet<BuildGraphNode> = hashSetOf()
    // single artifact producing node
    val producers = hashMapOf<ArtifactDesc, BuildGraphNode>()
    // multiple artifact consuming nodes
    val consumers = hashMapOf<ArtifactDesc, ArrayList<BuildGraphNode>>()

    protected fun add(node: BuildGraphNode) {
        val srcs = node.step.sources.toArrayList()
        val tgts = node.step.targets.toArrayList()
        // skip steps without inputs and outputs
        if (!srcs.isEmpty() || !tgts.isEmpty()) {
            nodes.add(node)
            producers.putAll(srcs.map { Pair(it, node) })
            for (it in tgts) {
                val lst = consumers.get(it)
                if (lst != null) lst.add(node)
                else consumers.put(it, arrayListOf(node))
            }
        }
    }

    public fun add(module: Module, scenarios: Scenarios = Scenarios.Same, selector: ScenarioSelector = ScenarioSelector.Any): BuildGraph {

        // \todo "replace" functionality
        val moduleFlavor = ModuleFlavor(module, scenarios)
        moduleSelectors.put(moduleFlavor, moduleSelectors.get(moduleFlavor)?.combine(selector) ?: selector)

        // processing dependency modules
        module.dependencies.forEach { add(it.module, it.scenarios, it.selector) }

        // processing nested modules; nested modules are visible outside the module, so results are added to the outgoingTargets
        module.children.forEach { add(it) }

        // processing own build steps - adding nodes that do not change scenario
        module.steps.forEach { step -> add(BuildGraphNode(ModuleFlavor(module, scenarios), step)) }
        return this
    }

    private fun isSelected(producingNode: BuildGraphNode, scenarios: Scenarios): Boolean =
        // check match with module and node selectors
        // \todo log skipped steps and reasons
        scenarios.matches(moduleSelectors.get(producingNode.moduleFlavor) ?:
                    throw Exception("incosistent graph: missing module for node $producingNode"))
            && scenarios.matches(producingNode.step.selector)

    private fun getProducingNode(it: ArtifactDesc, scenarios: Scenarios): BuildGraphNode? {
        val producingNode = producers.get(it) ?: throw Exception("incosistent graph: missing producer for $it")
        return if (isSelected(producingNode, scenarios)) producingNode else null
    }

    private fun getConsumingNodes(it: ArtifactDesc, scenarios: Scenarios): Iterable<BuildGraphNode> =
        consumers.get(it).filter { isSelected(it, scenarios) }

    // filtered inputs
    public fun sources(node: BuildGraphNode, scenarios: Scenarios): Iterable<ArtifactDesc> =
            if (scenarios.matches(node.step.selector))
                node.step.sources.filter { getProducingNode(it, scenarios) != null }
            else listOf()

    // filtered outputs
    public fun targets(node: BuildGraphNode, scenarios: Scenarios): Iterable<ArtifactDesc> =
            if (scenarios.matches(node.step.selector))
            // assuming that not all step's targets are consumed, so selecting only those that are
                node.step.targets.filter { !getConsumingNodes(it, scenarios).none() }
            else listOf()

    public fun prev(node: BuildGraphNode, scenarios: Scenarios): Iterable<BuildGraphNode> =
            if (scenarios.matches(node.step.selector))
                node.step.sources
                        .map { getProducingNode(it, scenarios) }
                        .filterNotNull()
                        .distinct()
            else listOf()

    public fun next(node: BuildGraphNode, scenarios: Scenarios): Iterable<BuildGraphNode> =
            if (scenarios.matches(node.step.selector))
                node.step.targets
                        .flatMap { getConsumingNodes(it, scenarios) }
                        .distinct()
            else listOf()
}


public fun BuildGraph(modules: Iterable<Module>): BuildGraph {

    val graph = BuildGraph()
    modules.forEach { graph.add(it) }
    return graph
}

// \todo - check the module scenario matching, but scenario changes should be taken into account
// (or may be better to use proper traversal from these targets to determine real ones,
//  or may be changing of scenarios is irrelevant, because all such a cases should be dependencies only, and thus
//    filtered out by !consumers.contains clause)

// returns target artifacts
// \todo targets from submodules?
public fun BuildGraph.targets(scenario: Scenarios): Iterable<ArtifactDesc> =
        nodes.filter { scenario.matches(it.step.selector) }
             .flatMap { if (it.step.export) targets(it, scenario)
                        else targets(it, scenario).filter { !consumers.contains(it) } }

// returns target artifacts
// \todo targets from submodules?
public fun BuildGraph.targets(modules: Iterable<Module>, scenario: Scenarios): Iterable<ArtifactDesc> {

    val modulesSet = modules.toHashSet()
    return nodes.filter { scenario.matches(it.step.selector) && modulesSet.contains(it.moduleFlavor) }
                .flatMap { if (it.step.export) targets(it, scenario)
                           else targets(it, scenario).filter { !consumers.contains(it) } }
}

public fun BuildGraph.roots(scenario: Scenarios): Iterable<BuildGraphNode> =
        nodes.filter { !sources(it, scenario).any { producers.contains(it) } }

public fun BuildGraph.leafs(scenario: Scenarios): Iterable<BuildGraphNode> =
        nodes.filter { !targets(it, scenario).any { consumers.contains(it) } }


// todo module flavor scenario extraction and handling (stack) on every step
public fun BuildGraph.buildPartialApply( scenario: Scenarios,
                                         buildFun: (BuildGraphNode) -> Boolean,
                                         sources: Set<BuildGraphNode> = this.roots(scenario).toHashSet(),
                                         targets: Iterable<BuildGraphNode> = this.leafs(scenario)) {
    subgraphDFS( from = targets,
                 to = sources,
                 preorderPred = { false },
                 postorderPred = buildFun,
                 nextNodes = {(n: BuildGraphNode) -> this.prev(n, scenario)},
                 checkTraversal = makeVisitedTraversalChecker<BuildGraphNode>())
}

public fun BuildGraph.buildAllApply(scenario: Scenarios, buildFun: (node: BuildGraphNode) -> Boolean) {
    graphDFS( from = leafs(scenario),
              preorderPred = { false },
              postorderPred = buildFun,
              nextNodes = {(n: BuildGraphNode) -> this.prev(n, scenario)},
              checkTraversal = makeVisitedTraversalChecker<BuildGraphNode>())
}


