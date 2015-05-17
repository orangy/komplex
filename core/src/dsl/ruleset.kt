
package komplex.dsl

import java.util.ArrayList
import komplex.model.combine
import komplex.model.ScenarioSelector


public open class RuleSetDesc(val rules: Iterable<Rule>) {}


public class ModuleRuleSet(val parent: Module) {
    val selectors: MutableList<ScenarioSelector> = arrayListOf()

    public val rules: MutableList<Rule> = arrayListOf()

    public fun invoke(vararg sels: ScenarioSelector): ModuleRuleSet {
        selectors.addAll(sels)
        return this
    }

    public fun invoke(body: ModuleRuleSet.() -> Unit) {
        body()
    }

    inline public fun invoke(vararg scenario: ScenarioSelector, body: ModuleRuleSet.() -> Unit) {
        invoke(*scenario)
        body()
    }

    public fun using<TR : Rule>(rule: TR): TR {
        rules.add(rule)
        rule.selector = selectors.combine(true)
        return rule
    }

    public fun using<TR : Rule>(rule: TR, body: TR.() -> Unit): TR {
        rule.body() // \todo find out how to write it
        return using(rule)
    }

    public fun using<TR : RuleSetDesc>(ruleSet: TR): TR {
        rules.addAll(ruleSet.rules)
        ruleSet.rules.forEach { it.selector = selectors.combine(true) }
        return ruleSet
    }

    public fun using<TR : RuleSetDesc>(ruleSet: TR, body: TR.() -> Unit): TR {
        ruleSet.body() // \todo find out how to write it
        return using(ruleSet)
    }


}
