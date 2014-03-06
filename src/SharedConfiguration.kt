package komplex

class SharedConfiguration(val pattern : String, val parent : Project) : ProjectConfiguration() {
    override val title: String
        get() = if (description.isEmpty()) pattern else "$pattern ($description)"

    override fun dump(block: Block, indent : String ) {
        super.dump(block, indent)
    }
}

