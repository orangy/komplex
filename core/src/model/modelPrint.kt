
package komplex.model

import komplex.utils.IndentLn
import komplex.utils.Named
import komplex.dsl.Artifact
import komplex.utils.SpaceIndent

public fun BuildGraphNode.nicePrint(indent: IndentLn, graph: BuildGraph? = null, scenario: Scenarios? = null): String =
        if (graph == null)
            step.nicePrint(indent)
        else
            step.nicePrint(indent, false) +
            "${indent.inc()}from:" +
            graph.sources(this, scenario!!).map { it.nicePrint(indent.inc(2)) }.joinToString() +
            "${indent.inc()}to:" +
            graph.targets(this, scenario).map { it.nicePrint(indent.inc(2)) }.joinToString()

public fun ArtifactDesc.nicePrint(indent: IndentLn): String = "$indent$name"

public fun Scenario.nicePrint(indent: IndentLn): String = "$indent$this"

public fun Scenarios.nicePrint(indent: IndentLn): String = "$indent" + when (this) {
    Scenarios.All -> "All"
    Scenarios.None -> "None"
    Scenarios.Default -> "Default"
    Scenarios.Same -> "Same"
    else -> this.items.map { it.nicePrint(indent) }
}

public fun ScenarioSelector.nicePrint(indent: IndentLn): String = "$indent" + when {
    this == ScenarioSelector.Any -> "Any"
    this == ScenarioSelector.None -> "None"
    else -> scenarios.nicePrint(indent)
}

public fun Step.nicePrint(indent: IndentLn, printInsOuts: Boolean = true): String =
        "$indent$name (${selector.nicePrint(SpaceIndent())})" +
        if (printInsOuts)
            (if (sources.none()) ""
             else "${indent.inc()}from:" + sources.map { it.nicePrint(indent.inc(2)) }.joinToString()) +
            (if (targets.none()) ""
             else "${indent.inc()}to:" + targets.map { it.nicePrint(indent.inc(2)) }.joinToString())
        else ""

public fun Module.nicePrint(indent: IndentLn): String =
        "$indent$name" +
        (if (steps.none()) ""
         else "${indent.inc()}steps:" + steps.map { it.nicePrint(indent.inc(2)) }.joinToString()) +
        (if (children.none()) ""
         else "${indent.inc()}modules:" +
        children.map { it.nicePrint(indent.inc(2)) }.joinToString())

public fun ModuleCollection.nicePrint(indent: IndentLn): String =
        children.map { it.nicePrint(indent.inc()) }.joinToString()

public fun BuildGraph.nicePrintAll( indent: IndentLn, scenario: Scenarios) {
    buildAllApply(scenario, { (n) -> println(n.nicePrint(indent, this)); false })
}

public fun BuildGraph.nicePrint( indent: IndentLn,
                                 scenario: Scenarios,
                                 sources: Set<BuildGraphNode> = this.roots(scenario).toHashSet(),
                                 targets: Iterable<BuildGraphNode> = this.leafs(scenario)) {
    buildPartialApply(scenario, { (n) -> println(n.nicePrint(indent, this)); false }, sources, targets)
}

