package komplex

import java.util.ArrayList

public class ModuleRuleSet(val parent: ModuleScript, val selectors: List<ScenarioSelector>) {
    public val rules: MutableList<Tool.Rule> = arrayListOf()

    public fun invoke(body: ModuleRuleSet.() -> Unit) {
        body()
    }

    public fun using<TR : Tool.Rule>(rule: TR): TR {
        rules.add(rule)
        return rule
    }

    public fun using<TR : Tool.Rule>(rule: TR, body: TR.() -> Unit): TR {
        rule.body() // \todo find out how to write it
        rules.add(rule)
        return rule
    }
}
