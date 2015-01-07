
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
        modules.add(node.module)
        nodes.add(node)
        incomingLinks.putAll( node.step.sources(scenario).map { Pair(it, node) })
        outgoingLinks.putAll( node.step.targets(scenario).map { Pair(it, node) })
    }

    // filtered inputs
    public fun sources(node: BuildGraphNode): Iterable<Artifact> =
            node.step.sources(scenario).filter { incomingLinks.contains(it) }

    // filtered outputs
    public fun targets(node: BuildGraphNode): Iterable<Artifact> =
            node.step.targets(scenario).filter { outgoingLinks.contains(it) }

    public fun prev(node: BuildGraphNode): Iterable<BuildGraphNode> =
            node.step.sources(scenario).filter { incomingLinks.contains(it) }.map { incomingLinks.get(it) }.distinct()

    public fun next(node: BuildGraphNode): Iterable<BuildGraphNode> =
            node.step.targets(scenario).filter { outgoingLinks.contains(it) }.map { outgoingLinks.get(it) }.distinct()
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
public fun BuildGraph.targets(): Iterable<Artifact> {

    return nodes.filter { !it.step.local }
                .flatMap { it.step.targets(scenario) }
}

// returns target artifacts
// \todo targets from submodules?
public fun BuildGraph.targets(modules: Iterable<Module>): Iterable<Artifact> {

    val modulesSet = modules.toHashSet()
    return nodes.filter { !it.step.local && modulesSet.contains(it.module) }
            .flatMap { it.step.targets(scenario) }
}


// these are more examples rather than a good set of wrappers
public fun BuildGraph.forwardBFS(from: Iterable<Artifact>,
                                 preorderPred: (edge: BuildGraphNode) -> Boolean = { true },
                                 postorderPred: (edge: BuildGraphNode) -> Boolean = { true },
                                 checkTraversal: (edge: BuildGraphNode) -> Boolean = makeVisitedTraversalChecker<BuildGraphNode>() ) {
    // note: incoming artifacts set could be a superset of graph's inputs
    graphBFS( from.map { if (incomingLinks.contains(it)) incomingLinks.get(it) else null }.filterNotNull().distinct(),
              preorderPred,
              postorderPred,
              {(n: BuildGraphNode) -> this.next(n) },
              checkTraversal)
}

public fun BuildGraph.backwardBFS(from: Iterable<Artifact>,
                                  preorderPred: (edge: BuildGraphNode) -> Boolean = { true },
                                  postorderPred: (edge: BuildGraphNode) -> Boolean = { true },
                                  checkTraversal: (edge: BuildGraphNode) -> Boolean = makeVisitedTraversalChecker<BuildGraphNode>() ) {
    // note: here all outputs should be strictly in the graph
    graphBFS( from.map { outgoingLinks.get(it) }.distinct(),
              preorderPred,
              postorderPred,
              {(n: BuildGraphNode) -> this.prev(n)},
              checkTraversal)
}

public fun BuildGraph.forwardDFS(from: Iterable<Artifact>,
                                 preorderPred: (edge: BuildGraphNode) -> Boolean = { true },
                                 postorderPred: (edge: BuildGraphNode) -> Boolean = { true },
                                 checkTraversal: (edge: BuildGraphNode) -> Boolean = makeVisitedTraversalChecker<BuildGraphNode>() ) {
    // note: incoming artifacts set could be a superset of graph's inputs
    graphDFS( from.map { if (incomingLinks.contains(it)) incomingLinks.get(it) else null }.filterNotNull().distinct(),
              preorderPred,
              postorderPred,
              {(n: BuildGraphNode) -> this.next(n)},
              checkTraversal)
}
public fun BuildGraph.backwardDFS(from: Iterable<Artifact>,
                                  preorderPred: (edge: BuildGraphNode) -> Boolean = { true },
                                  postorderPred: (edge: BuildGraphNode) -> Boolean = { true },
                                  checkTraversal: (edge: BuildGraphNode) -> Boolean = makeVisitedTraversalChecker<BuildGraphNode>() ) {
    // note: here all outputs should be strictly in the graph
    graphDFS( from.map { outgoingLinks.get(it) }.distinct(),
              preorderPred,
              postorderPred,
              {(n: BuildGraphNode) -> this.prev(n)},
              checkTraversal)
}

public fun BuildGraph.print() {
    backwardBFS( this.targets(),
                 preorderPred = { (e) -> e.print(this); false })
}

public fun BuildGraphNode.print(graph: BuildGraph) {
    println("${step.tool.title}(${module.title})")
    println("  from:")
    graph.sources(this).forEach { it.print("    ") }
    println("  to:")
    graph.targets(this).forEach { it.print("    ") }
}

