package komplex

public data class BuildStep(public val scenario: Scenario,
                            public val module: Module,
                            public val rule: ModuleRule) {
    public fun execute(): BuildResult = rule.execute(this)

}