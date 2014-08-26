package komplex

public data class BuildContext(public val scenario: Scenario,
                               public val module: Module,
                               public val step: ModuleRule) {

}