package komplex

fun String.shift(): String = this + "  "

public fun ModuleCollection.print(indent: String) {
    for (child in modules) {
        println()
        println("$indent Module: ${child.title}")
        child.print(indent.shift())
    }
}

public fun Module.print(indent: String) {
    println("$indent Version: $version")
    depends.print(indent)
    build.print(indent)
    (this : ModuleCollection).print(indent)
}

public fun ModuleScenario.print(indent: String) {
    println("$indent Scenario $scenarios")
    for (rule in rules) {
        rule.print(indent.shift())
    }
}

public fun ModuleScript.print(indent: String) {
    for (scenario in scenarios) {
        scenario.print(indent)
    }
}

public fun Dependencies.print(indent: String) {
    if (dependencies.size == 0)
        return

    println("$indent Depends on")
    for ((scenario, reference) in dependencies) {
        println("$indent Module: ${reference} (in ${scenario})")
    }
}

public fun ModuleRule.print(indent: String) {
    when (this) {
        is ModuleToolRule<*> -> print(indent)
        else -> println("$indent Rule: $this")
    }
}

public fun ModuleToolRule<*>.print(indent: String) {
    println("$indent Rule \"${tool.title}\"")
    tool.print(indent.shift())
}

public fun Tool.print(indent: String) {
    if (this is ConsumingTool) print(indent)
    if (this is ProducingTool) print(indent)
}

public fun Artifact.print(indent: String) {
    println("$indent ${toString()}")
}

public fun ConsumingTool.print(indent: String) {
    println("$indent From: ")
    for (artifact in sources)
        artifact.print(indent.shift())
}

public fun ProducingTool.print(indent: String) {
    println("$indent To: ")
    for (artifact in destinations)
        artifact.print(indent.shift())
}

