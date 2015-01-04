package komplex

public fun BuildPlan.print(indent: String) {
    for(step in steps)
        step.print(indent)
}

public fun BuildStepContext.print(indent: String) {
    println("$indent ${scenario.name} $module")
}
