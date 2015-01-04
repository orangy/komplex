package komplex

/**
 * Collection of (scenarios)[Scenario] for the [module]
 */
public class ModuleScript(val module: Module) {
    public val ruleSets: MutableList<ModuleRuleSet> = arrayListOf()

    public fun using<TR : Tool.Rule>(rule: TR): TR = invoke(ScenarioSelector("*")).using(rule)

    inline public fun invoke(vararg scenario: ScenarioSelector, body: ModuleRuleSet.() -> Unit): ModuleRuleSet {
        val ruleSet = invoke(*scenario)
        ruleSet.body()
        return ruleSet
    }

    public fun invoke(vararg config: ScenarioSelector): ModuleRuleSet {
        val ruleSet = ModuleRuleSet(this, config.toList())
        ruleSets.add(ruleSet)
        return ruleSet
    }
}