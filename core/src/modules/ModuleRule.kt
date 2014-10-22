package komplex

public abstract class ModuleRule(val parent: ModuleRuleSet) {
    public abstract fun execute(context: BuildStep): BuildResult
    public abstract fun targets(): List<Artifact>
}

public class ModuleToolRule<TTool : Tool>(parent: ModuleRuleSet, val tool: TTool) : ModuleRule(parent) {
    public override fun execute(context: BuildStep): BuildResult {
        try {
            return tool.execute(context)
        } catch (e: Throwable) {
            e.printStackTrace()
            return BuildResult.Fail
        }
    }
    public override fun targets(): List<Artifact> {
        var res = arrayListOf<Artifact>()
        if (tool is ProducingTool) res.addAll(tool.destinations)
        return res
    }

    override fun toString(): String = "with ${tool.title}"
}
