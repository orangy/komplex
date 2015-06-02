
package komplex.model

import komplex.*
import komplex.dsl.FolderArtifact
import komplex.tools.CopyToolRule
import komplex.utils.*
import java.util.HashSet
import java.util.HashMap
import java.util.ArrayList
import kotlin.platform.platformName

public data class ModuleFlavor(public val module: Module,
                               public val scenarios: Scenarios) {}

public data class BuildGraphNode(public val moduleFlavor: ModuleFlavor, public val step: Step) {
    override fun toString(): String = "[${moduleFlavor.module.name}.${step.name}](${step.selector.scenarios})"
}

platformName("producers_map_contains")
internal fun HashMap<ArtifactDesc, BuildGraphNode>.contains(artifact: ArtifactDesc, scenario: Scenarios): Boolean {
    val node = get(artifact)
    return if (node != null) scenario.matches(node.step.selector) else false
}

platformName("consumers_map_contains")
internal fun HashMap<ArtifactDesc, ArrayList<BuildGraphNode>>.contains(artifact: ArtifactDesc, scenario: Scenarios): Boolean =
    get(artifact)?.any { scenario.matches(it.step.selector) } ?: false


public class BuildGraph() {

    val moduleSelectors: HashMap<ModuleFlavor, ScenarioSelector> = hashMapOf()
    // all nodes index
    val nodes: HashSet<BuildGraphNode> = hashSetOf()
    // single artifact producing node
    val producers: HashMap<ArtifactDesc, BuildGraphNode> = hashMapOf()
    // multiple artifact consuming nodes
    val consumers: HashMap<ArtifactDesc, ArrayList<BuildGraphNode>> = hashMapOf()


    protected fun add(node: BuildGraphNode) {

        val srcs = node.step.sources.toArrayList()
        val tgts = node.step.targets.toArrayList()
        // skip steps without inputs and outputs
        if (!srcs.isEmpty() || !tgts.isEmpty()) {
            nodes.add(node)

            for (tgt in tgts) {
                // \todo MT-safety, if needed
                val n = producers.get(tgt)
                if (n != null)
                    throw Exception("Error cannot add $node, because $n produces the same artifact $tgt")
                producers.put(tgt, node)
            }
            for (it in srcs) {
                val lst = consumers.get(it)
                if (lst != null) lst.add(node)
                else consumers.put(it, arrayListOf(node))
            }
        }
    }


    public fun add(module: Module, scenarios: Scenarios = Scenarios.Same, selector: ScenarioSelector = ScenarioSelector.Any): BuildGraph {

        // \todo "replace" functionality
        val moduleFlavor = ModuleFlavor(module, scenarios)
        val previousSel = moduleSelectors.get(moduleFlavor)
        val newSel = previousSel?.combine(selector) ?: selector
        if (previousSel == null || previousSel != newSel) {

            if (previousSel == null)
                log.trace("Adding module ${module.fullName} for scenario(s) $newSel")
            else
                log.trace("Updating module ${module.fullName} for scenario(s) $newSel")

            moduleSelectors.put(moduleFlavor, newSel)

            // processing dependency modules
            module.dependencies.forEach { add(it.module, it.scenarios, it.selector) }

            // processing nested modules; nested modules are visible outside the module, so results are added to the outgoingTargets
            module.children.forEach { add(it) }

            // processing own build steps - adding nodes that do not change scenario
            module.steps.forEach { step -> add(BuildGraphNode(ModuleFlavor(module, scenarios), step)) }

            val matchingScenarios = if (scenarios.resolved()) scenarios else Scenarios.All
            if (module.targets(matchingScenarios).none())
                log.info("Warning: module ${module.fullName} do not export anything in scenario(s) $matchingScenarios")
        }
        else
            log.trace("Module ${module.fullName} is already added for scenario(s) $previousSel")
        return this
    }


    private fun isSelected(producingNode: BuildGraphNode, scenarios: Scenarios): Boolean {
        // check match with module and node selectors
        val moduleSel = moduleSelectors.get(producingNode.moduleFlavor ?:
                throw Exception("incosistent graph: missing module for node $producingNode"))
        if (!scenarios.matches(moduleSel))
            log.trace("skipping $producingNode because it's module selector $moduleSel doesn't match with $scenarios")
        else if (!scenarios.matches(producingNode.step.selector))
            log.trace("skipping $producingNode because it itself doesn't match with $scenarios")
        else return true
        return false;
    }


    public fun getProducingNode(it: ArtifactDesc, scenarios: Scenarios): BuildGraphNode? {
        val producingNode = producers.get(it)// ?: throw Exception("incosistent graph: missing producer for $it")
        return if (producingNode != null && isSelected(producingNode, scenarios)) producingNode else null
    }


    public fun getConsumingNodes(it: ArtifactDesc, scenarios: Scenarios): Iterable<BuildGraphNode> =
        consumers.get(it)?.filter { isSelected(it, scenarios) } ?: listOf()


    // filtered inputs
    public fun sources(node: BuildGraphNode, scenarios: Scenarios): Iterable<ArtifactDesc> =
            if (scenarios.matches(node.step.selector))
                // all node sources are required
                node.step.sources
            else listOf()


    // filtered outputs
    public fun targets(node: BuildGraphNode, scenarios: Scenarios): Iterable<ArtifactDesc> =
            if (scenarios.matches(node.step.selector))
                // all node targets
                node.step.targets
            else listOf()


    public fun prev(node: BuildGraphNode, scenarios: Scenarios): Iterable<BuildGraphNode> =
            if (scenarios.matches(node.step.selector)) {
                val prev = node.step.sources
                        .map { getProducingNode(it, scenarios) }
                        .filterNotNull()
                        .distinct()
                //log.trace("prev nodes for $node($scenarios): ${prev.joinToString(", ", "(", ")")}")
                prev
            }
            else {
                log.trace("skip $node($scenarios): no matching scenarios")
                listOf<BuildGraphNode>()
            }


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
                        else targets(it, scenario).filter { !consumers.contains(it, scenario) } }

// returns target artifacts
// \todo targets from submodules?
public fun BuildGraph.targets(modules: Iterable<Module>, scenario: Scenarios): Iterable<ArtifactDesc> {

    val modulesSet = modules.toHashSet()
    return nodes.filter { scenario.matches(it.step.selector) && modulesSet.contains(it.moduleFlavor) }
                .flatMap { if (it.step.export) targets(it, scenario)
                           else targets(it, scenario).filter { !consumers.contains(it, scenario) } }
}

public fun BuildGraph.roots(scenario: Scenarios): Iterable<BuildGraphNode> {
    val roots = nodes.filter {
        if (scenario.matches(it.step.selector)) {
            val areSourcesProduced = sources(it, scenario).any { producers.contains(it, scenario) }
            if (log.isTraceEnabled() && areSourcesProduced)
                log.trace("filtered out $it from roots because of produced source artifacts ${
                    sources(it, scenario).filter { producers.contains(it) }.joinToString(", ", "(", ")")}")
            !areSourcesProduced
        } else false
    }
    log.trace("roots: ${roots.joinToString(", ", "(", ")")}")
    return roots
}


public fun BuildGraph.leafs(scenario: Scenarios): Iterable<BuildGraphNode> {
    return nodes.filter { scenario.matches(it.step.selector) &&
                          targets(it, scenario).none { consumers.contains(it, scenario) } }
}


// todo module flavor scenario extraction and handling (stack) on every step
public fun BuildGraph.buildPartialApply( scenario: Scenarios,
                                         buildFun: (BuildGraphNode) -> Boolean,
                                         sources: Set<BuildGraphNode> = this.roots(scenario).toHashSet(),
                                         targets: Iterable<BuildGraphNode> = this.leafs(scenario)) {
    subgraphDFS( from = targets,
                 to = sources,
                 preorderPred = { false },
                 postorderPred = buildFun,
                 nextNodes = { n: BuildGraphNode -> this.prev(n, scenario)},
                 checkTraversal = makeVisitedTraversalChecker<BuildGraphNode>())
}

public fun BuildGraph.buildAllApply(scenario: Scenarios, buildFun: (node: BuildGraphNode) -> Boolean) {
    graphDFS( from = leafs(scenario),
              preorderPred = { false },
              postorderPred = buildFun,
              nextNodes = { n: BuildGraphNode -> this.prev(n, scenario)},
              checkTraversal = makeVisitedTraversalChecker<BuildGraphNode>())
}


