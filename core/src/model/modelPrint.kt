
package komplex.model

import komplex.utils.IndentLn
import komplex.utils.Named
import komplex.dsl.Artifact
import komplex.utils.SpaceIndent

public fun BuildGraphNode.nicePrint(indent: IndentLn, graph: BuildGraph? = null, scenario: Scenarios = Scenarios.All): String =
        if (graph == null)
            step.nicePrint(indent, moduleName = moduleFlavor.module.fullName, printInsOuts = true)
        else
            step.nicePrint(indent, moduleName = moduleFlavor.module.fullName, printInsOuts = false) +
            nicePrintPins(indent, graph.sources(this, scenario), "from:", pinDescFn = {
                val prod = graph.getProducingNode(it,scenario);
                if (prod != null) "produced by $prod" else "(external)" } ) +
            nicePrintPins(indent, graph.targets(this, scenario), "to:")

public fun ArtifactDesc.nicePrint(indent: IndentLn): String = "$indent$name"

public fun Scenario.nicePrint(indent: IndentLn): String = "$indent$this"

public fun Scenarios.nicePrint(indent: IndentLn): String = "$indent" + when (this) {
    Scenarios.All -> "All"
    Scenarios.None -> "None"
    Scenarios.Default_ -> "Default"
    Scenarios.Same -> "Same"
    else -> this.items.map { it.nicePrint(indent) }
}

public fun ScenarioSelector.nicePrint(indent: IndentLn): String = "$indent" + when {
    this == ScenarioSelector.Any -> "Any"
    this == ScenarioSelector.None -> "None"
    else -> scenarios.nicePrint(indent)
}

public fun Step.nicePrint(indent: IndentLn, moduleName: String = "", printInsOuts: Boolean = true): String =
        indent.toString() +
        (if (moduleName.length() > 0) "[$moduleName] " else "") +
        name +
        (if (export) " - export - " else "") +
        "(${selector.nicePrint(SpaceIndent())})" +
        (if (printInsOuts)
            nicePrintPins(indent, sources, "from:") + nicePrintPins(indent, targets, "to:")
         else "")

private fun nicePrintPins(indent: IndentLn, pins: Iterable<ArtifactDesc>, prefix: String, pinDescFn: ((ArtifactDesc) -> String)? = null): String {
    return (if (pins.none()) ""
    else "${indent.inc()}$prefix" + pins.map { it.nicePrint(indent.inc(2)) + (if (pinDescFn!=null) " -- ${pinDescFn(it)}" else "") }.joinToString())
}

public fun Module.nicePrint(indent: IndentLn): String =
        "$indent$name" +
        (if (steps.none()) ""
         else "${indent.inc()}steps:" + steps.map { it.nicePrint(indent.inc(2)) }.joinToString()) +
        (if (children.none()) ""
         else "${indent.inc()}modules:" + children.map { it.nicePrint(indent.inc(2)) }.joinToString()) +
        (if (dependencies.none()) ""
         else "${indent.inc()}depends on: " + dependencies.map { it.module.name }.joinToString(", "))

public fun ModuleCollection.nicePrint(indent: IndentLn): String =
        children.map { it.nicePrint(indent.inc()) }.joinToString()

public fun BuildGraph.nicePrintAll( indent: IndentLn, scenario: Scenarios) {
    buildAllApply(scenario, { n -> println(n.nicePrint(indent, this)); false })
}

public fun BuildGraph.nicePrint( indent: IndentLn,
                                 scenario: Scenarios,
                                 sources: Set<BuildGraphNode> = this.roots(scenario).toHashSet(),
                                 targets: Iterable<BuildGraphNode> = this.leafs(scenario)) {
    buildPartialApply(scenario, { n -> println(n.nicePrint(indent, this)); false }, sources, targets)
}

