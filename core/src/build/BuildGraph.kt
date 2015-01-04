
package komplex

import komplex.BuildStepContext
import komplex.Artifact
import java.util.HashMap
import java.util.HashSet
import komplex.Tool.Rule

// komplex build graph edge and graph implementations
public class BuildGraphEdge(public val module: Module? = null,
                            public val target: Artifact? = null,
                            public val source: Artifact? = null,
                            public val step: Rule? = null) : DagEdge {
    override fun makeInverse(): BuildGraphEdge = BuildGraphEdge(module, source, target, step)
    override fun equals(other: Any?): Boolean = when (other) {
        is BuildGraphEdge -> target == other.target && source == other.source
        else -> false
    }
    override fun hashCode(): Int = (target?.hashCode() ?: 1) * (source?.hashCode() ?: 1)
}

public class BuildGraph(): Graph<BuildGraphEdge> {

    val modules: HashSet<Module> = hashSetOf()
    val incomingLinks = hashMapOf<Artifact, HashSet<BuildGraphEdge>>()
    val outgoingLinks = hashMapOf<Artifact, HashSet<BuildGraphEdge>>()

    public override fun add(edge: BuildGraphEdge) {
        modules.add(edge.module)
        if (!incomingLinks.containsKey(edge.target)) incomingLinks.put(edge.target, hashSetOf(edge))
        else incomingLinks[edge.target].add(edge)
        if (!outgoingLinks.containsKey(edge.source)) outgoingLinks.put(edge.source, hashSetOf(edge))
        else outgoingLinks[edge.source].add(edge)
    }

    public override fun incoming(edge: BuildGraphEdge): Iterable<BuildGraphEdge>? = if (edge.source != null) incomingLinks[edge.source] else null
    public override fun outgoing(edge: BuildGraphEdge): Iterable<BuildGraphEdge>? = if (edge.target != null) outgoingLinks[edge.target] else null
}


fun BuildGraph.add(module: Module, scenario: Scenario): BuildGraph {

    // processing dependency modules
    module.depends.modules(scenario).forEach { add(it, scenario) }

    // processing nested modules; nested modules are visible outside the module, so results are added to the outgoingTargets
    module.modules.forEach { add(it, scenario) }

    // processing own build steps
    for (ruleSet in module.build.ruleSets) {
        if (ruleSet.selectors.any { it.matches(scenario) }) {
            for (step in ruleSet.rules) {
                val context = BuildStepContext(scenario, module)
                val targets = step.targets(context.scenario);
                when (targets.count()) {
                    0 -> step.sources(context.scenario).map {(s) -> add(BuildGraphEdge(module = module, source = s, step = step)) }
                    else -> step.targets(context.scenario).map {(t) -> step.sources(context.scenario).map {(s) -> add(BuildGraphEdge(module = module, target = t, source = s, step = step)) } }
                }
            }
        }
    }
    return this
}


public fun buildGraph(modules: Iterable<Module>, scenario: Scenario): BuildGraph {

    val graph = BuildGraph()
    modules.forEach { graph.add(it, scenario) }
    return graph
}


// returns target artifacts
// \todo targets from submodules?
public fun targets(module: Module, graph: BuildGraph): Iterable<Artifact> {

    return if (graph.modules.contains(module))
                graph.outgoingLinks
                .flatMap { it.getValue() }
                .filter { it.module == module && it.step != null && it.step!!.local }
                .map { it.target }
                .filterNotNull()
            else listOf()
}

// examples?
public fun BuildGraph.forwardBFS(start: Artifact,
                                 func: (edge: BuildGraphEdge) -> Boolean,
                                 checkTraversal: (edge: BuildGraphEdge) -> Boolean = makeDagAsserter<BuildGraphEdge>() ) {
    graphBFS(BuildGraphEdge(target = start), func, {(e: BuildGraphEdge) -> this.outgoing(e)}, checkTraversal)
}

public fun BuildGraph.backwardBFS(start: Artifact,
                                  func: (edge: BuildGraphEdge) -> Boolean,
                                  checkTraversal: (edge: BuildGraphEdge) -> Boolean = makeDagAsserter<BuildGraphEdge>() ) {
    graphBFS(BuildGraphEdge(source = start), func, {(e: BuildGraphEdge) -> this.incoming(e)}, checkTraversal)
}

public fun BuildGraph.forwardDFS(start: Artifact,
                                 func: (edge: BuildGraphEdge) -> Boolean,
                                 checkTraversal: (edge: BuildGraphEdge) -> Boolean = makeDagAsserter<BuildGraphEdge>() ) {
    graphDFS(BuildGraphEdge(target = start), func, {(e: BuildGraphEdge) -> this.outgoing(e)}, checkTraversal)
}
public fun BuildGraph.backwardDFS(start: Artifact,
                                  func: (edge: BuildGraphEdge) -> Boolean,
                                  checkTraversal: (edge: BuildGraphEdge) -> Boolean = makeDagAsserter<BuildGraphEdge>() ) {
    graphDFS(BuildGraphEdge(source = start), func, {(e: BuildGraphEdge) -> this.incoming(e)}, checkTraversal)
}

public fun BuildGraph.print(start: Artifact) {
    forwardBFS(start, { (e) -> print("$e -> ${e.target}"); false }, makeVisitedTraversalChecker<BuildGraphEdge>())
}


