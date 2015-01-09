package komplex

// represents tool itself
public trait Tool {
    val title: String

    // represents tool rule configuration
    public trait Rule {
        val tool: Tool
        val export: Boolean // defines if step targets are local or exported from the module
        public fun sources(scenario: Scenario): Iterable<Artifact> = listOf()
        public fun targets(scenario: Scenario): Iterable<Artifact> = listOf()
        public fun valid(scenario: Scenario): Boolean = true

        public fun execute(context: BuildStepContext): BuildResult = tool.execute(context, this)
    }

    public fun execute(context: BuildStepContext, rule: Rule): BuildResult
}


public fun <TR : Tool.Rule> TR.with(body: TR.() -> Unit): TR {
    body()
    return this
}

// in fact - tool rules are stored in the singleton
public object tools {}


// helpers

public trait SelectArtifactsListTrait {
    val v: MutableCollection<Function1<Scenario, Iterable<Artifact>>>
    fun add(vararg selArtifacts: Iterable<(Scenario) -> Iterable<Artifact>>) =
            selArtifacts.forEach {
                v.addAll(it) }
    fun add(vararg selArtifacts: (Scenario) -> Iterable<Artifact>) =
            v.addAll(selArtifacts)
    // \todo find a way to add a second overload with Iterable
    // fun add(vararg artifacts: Iterable<Artifact>) = artifacts.forEach { add({(scenario: Scenario) -> it }) }
    fun add(vararg artifacts: Artifact) =
            add({(scenario: Scenario) -> artifacts.toArrayList() })
    fun add(vararg rules: Tool.Rule) =
            rules.forEach {
                add( { (scenario: Scenario) -> it.targets(scenario) }) }
    fun get(scenario: Scenario): Iterable<Artifact> = v.flatMap { it(scenario) }
}

public class SelectArtifactsList : SelectArtifactsListTrait {
    override val v = hashSetOf<Function1<Scenario, Iterable<Artifact>>>()
}
