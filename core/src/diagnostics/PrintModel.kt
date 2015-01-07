package komplex

// \todo implement generic set of funs with the signatures:
// fun T.nicePrint(delimiter = "\n  ", nextDelimiter = { shift(it) }, depth = -1): String

fun String.shift(): String = this + "  "

public fun ModuleCollection.print(indent: String) {
    for (child in modules) {
        child.print(indent.shift())
    }
}

public fun Module.print(indent: String) {
    println()
    println("$indent Module: $title ($version)")
    depends.print(indent)
    build.print(indent)
    (this : ModuleCollection).print(indent)
}

public fun ModuleRuleSet.print(indent: String) {
    println("$indent Scenario $selectors")
    for (rule in rules) {
        rule.print(indent.shift())
    }
}

public fun ModuleScript.print(indent: String) {
    for (scenario in ruleSets) {
        scenario.print(indent)
    }
}

public fun Dependencies.print(indent: String) {
    if (groups.size() == 0)
        return

    // \todo scenario-specific deps
    println("$indent Depends on modules:")
    groups.flatMap { it.moduleDeps }.forEach { println("${indent.shift()} ${it.moduleName}") }
    println("$indent Depends on libs:")
    groups.flatMap { it.artifactDeps }.forEach { it.print(indent.shift()) }
}

public fun Tool.Rule.print(indent: String) {
    println("$indent ${tool.title}")
    (this as? ConsumingTool.Rule)?.print(indent.shift())
    (this as? ProducingTool.Rule)?.print(indent.shift())
}

public fun Artifact.print(indent: String) {
    when (this) {
        is LibraryReferenceArtifact -> println("$indent ${mavenId().toString()}")
        is LibraryWithDependenciesArtifact -> {
            println("$indent [$id]")
            resolvedPaths.forEach { println("${indent.shift()} $it") }
        }
        else -> println("$indent ${toString()}")
    }
}

public fun ConsumingTool.Rule.print(indent: String) {
    println("$indent from: ")
    for (artifact in sources(Scenario("*")))
        artifact.print(indent.shift())
}

public fun ProducingTool.Rule.print(indent: String) {
    println("$indent to: ")
    for (artifact in targets(Scenario("*")))
        artifact.print(indent.shift())
}

