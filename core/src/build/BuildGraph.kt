
package komplex

import komplex.BuildStepContext
import komplex.Artifact
import java.util.HashMap
import java.util.HashSet
import komplex.Tool.Rule

// komplex build graph and graph node implementations
public data class BuildGraphNode(public val module: Module, public val step: Rule) {}

public class BuildGraph(public val scenario: Scenario): Graph<BuildGraphNode> {

    val modules: HashSet<Module> = hashSetOf()
    val nodes: HashSet<BuildGraphNode> = hashSetOf()
    val incomingLinks = hashMapOf<Artifact, BuildGraphNode>()
    val outgoingLinks = hashMapOf<Artifact, BuildGraphNode>()

    public override fun add(node: BuildGraphNode) {
        val srcs = node.step.sources(scenario).toArrayList()
        val tgts = node.step.targets(scenario).toArrayList()
        // skip steps without inputs and outputs
        if (!srcs.isEmpty() || !tgts.isEmpty()) {
            modules.add(node.module)
            nodes.add(node)
            incomingLinks.putAll(srcs.map { Pair(it, node) })
            outgoingLinks.putAll(tgts.map { Pair(it, node) })
        }
    }

    // filtered inputs
    public fun sources(node: BuildGraphNode): Iterable<Artifact> =
            node.step.sources(scenario).filter { incomingLinks.contains(it) }

    // filtered outputs
    public fun targets(node: BuildGraphNode): Iterable<Artifact> =
            node.step.targets(scenario).filter { outgoingLinks.contains(it) }

    public fun prev(node: BuildGraphNode): Iterable<BuildGraphNode> =
            node.step.sources(scenario).map { outgoingLinks.get(it) }.filterNotNull().distinct()

    public fun next(node: BuildGraphNode): Iterable<BuildGraphNode> =
            node.step.targets(scenario).map { incomingLinks.get(it) }.filterNotNull().distinct()
}


fun BuildGraph.add(module: Module): BuildGraph {

    // processing dependency modules
    module.depends.modules(scenario).forEach { add(it) }

    // processing nested modules; nested modules are visible outside the module, so results are added to the outgoingTargets
    module.modules.forEach { add(it) }

    // processing own build steps
    module.build.ruleSets
            .filter { it.selectors.any { it.matches(scenario) }}
            .flatMap { it.rules }
            .forEach { step -> add(BuildGraphNode(module, step))}
    return this
}


public fun BuildGraph(modules: Iterable<Module>, scenario: Scenario): BuildGraph {

    val graph = BuildGraph(scenario)
    modules.forEach { graph.add(it) }
    return graph
}


// returns target artifacts
// \todo targets from submodules?
public fun BuildGraph.targets(): Iterable<Artifact> =
        nodes.flatMap { if (it.step.export) targets(it) else targets(it).filter { !incomingLinks.contains(it) } }

// returns target artifacts
// \todo targets from submodules?
public fun BuildGraph.targets(modules: Iterable<Module>): Iterable<Artifact> {

    val modulesSet = modules.toHashSet()
    return nodes.filter { modulesSet.contains(it.module) }
            .flatMap { if (it.step.export) targets(it) else targets(it).filter { !incomingLinks.contains(it) } }
}

public fun BuildGraph.roots(): Iterable<BuildGraphNode> =
        nodes.filter { !sources(it).any { outgoingLinks.contains(it) } }

public fun BuildGraph.leafs(): Iterable<BuildGraphNode> =
        nodes.filter { !targets(it).any { incomingLinks.contains(it) } }


// these are more examples rather than a good set of wrappers
public fun BuildGraph.forwardBFS(from: Iterable<Artifact>,
                                 preorderPred: (node: BuildGraphNode) -> Boolean = { true },
                                 postorderPred: (node: BuildGraphNode) -> Boolean = { true },
                                 checkTraversal: (node: BuildGraphNode) -> Boolean = makeVisitedTraversalChecker<BuildGraphNode>() ) {
    // note: incoming artifacts set could be a superset of graph's inputs
    graphBFS( from.map { if (incomingLinks.contains(it)) incomingLinks.get(it) else null }.filterNotNull().distinct(),
              preorderPred,
              postorderPred,
              {(n: BuildGraphNode) -> this.next(n) },
              checkTraversal)
}

public fun BuildGraph.backwardBFS(from: Iterable<Artifact>,
                                  preorderPred: (node: BuildGraphNode) -> Boolean = { true },
                                  postorderPred: (node: BuildGraphNode) -> Boolean = { true },
                                  checkTraversal: (node: BuildGraphNode) -> Boolean = makeVisitedTraversalChecker<BuildGraphNode>() ) {
    // note: here all outputs should be strictly in the graph
    graphBFS( from.map { outgoingLinks.get(it) }.distinct(),
              preorderPred,
              postorderPred,
              {(n: BuildGraphNode) -> this.prev(n)},
              checkTraversal)
}

public fun BuildGraph.forwardDFS(from: Iterable<Artifact>,
                                 preorderPred: (node: BuildGraphNode) -> Boolean = { true },
                                 postorderPred: (node: BuildGraphNode) -> Boolean = { true },
                                 checkTraversal: (node: BuildGraphNode) -> Boolean = makeVisitedTraversalChecker<BuildGraphNode>() ) {
    // note: incoming artifacts set could be a superset of graph's inputs
    graphDFS( from.map { if (incomingLinks.contains(it)) incomingLinks.get(it) else null }.filterNotNull().distinct(),
              preorderPred,
              postorderPred,
              {(n: BuildGraphNode) -> this.next(n)},
              checkTraversal)
}

public fun BuildGraph.backwardDFS(from: Iterable<Artifact>,
                                  preorderPred: (node: BuildGraphNode) -> Boolean = { true },
                                  postorderPred: (node: BuildGraphNode) -> Boolean = { true },
                                  checkTraversal: (node: BuildGraphNode) -> Boolean = makeVisitedTraversalChecker<BuildGraphNode>() ) {
    // note: here all outputs should be strictly in the graph
    graphDFS( from.map { outgoingLinks.get(it) }.distinct(),
              preorderPred,
              postorderPred,
              {(n: BuildGraphNode) -> this.prev(n)},
              checkTraversal)
}

public fun BuildGraph.print() {
    graphBFS( roots(),
            { (e) -> e.print(this); false },
            { false },
            {(n: BuildGraphNode) -> next(n)},
            makeVisitedTraversalChecker<BuildGraphNode>())
}

public fun BuildGraphNode.print(graph: BuildGraph) {
    println("${step.tool.title}(${module.title})")
    println("  from:")
    graph.sources(this).forEach { it.print("    ") }
    println("  to:")
    graph.targets(this).forEach { it.print("    ") }
}

public fun BuildGraph.buildAllApply(buildFun: (node: BuildGraphNode) -> Boolean) {
    //backwardBFS(to, postorderPred = buildFun)
    graphDFS( leafs(),
            { false },
            buildFun,
            {(n: BuildGraphNode) -> this.prev(n)},
            makeVisitedTraversalChecker<BuildGraphNode>())
}

public fun BuildGraph.printBuildPlan() {
    buildAllApply({ (n) -> n.print(this); false })
}

public fun BuildGraph.build() {
    buildAllApply({ (n) ->
        val context = BuildStepContext(scenario, n.module)
        val result = n.step.execute(context)
        result != BuildResult.Success })
}
