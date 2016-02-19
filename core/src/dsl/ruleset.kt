
package komplex.dsl

import komplex.model.ScenarioSelector
import komplex.model.combine


open class RuleSetDesc(val rules: Iterable<Rule>) : GenericSourceType {}


class ModuleRuleSet(val parent: ProjectModule) {
    val selectors: MutableList<ScenarioSelector> = arrayListOf()

    val rules: MutableList<Rule> = arrayListOf()

    operator fun invoke(vararg sels: ScenarioSelector): ModuleRuleSet {
        selectors.addAll(sels)
        return this
    }

    fun invoke(body: ModuleRuleSet.() -> Unit) {
        body()
    }

    inline fun invoke(vararg scenario: ScenarioSelector, body: ModuleRuleSet.() -> Unit) {
        invoke(*scenario)
        body()
    }

    infix fun <TR : Rule> using(rule: TR): TR {
        rules.add(rule)
        rule.selector = selectors.combine(true)
        rule.module = parent
        parent.addConfigurable(rule)
        return rule
    }

    fun <TR : Rule> using(rule: TR, body: TR.() -> Unit): TR {
        rule.body() // \todo find out how to write it
        return using(rule)
    }

    infix fun <TR : RuleSetDesc> using(ruleSet: TR): TR {
        ruleSet.rules.forEach { using(it) }
        return ruleSet
    }

    fun <TR : RuleSetDesc> using(ruleSet: TR, body: TR.() -> Unit): TR {
        ruleSet.body() // \todo find out how to write it
        return using(ruleSet)
    }


}
