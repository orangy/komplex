package komplex

class SharedSettings(val pattern: String,
                     val parent: Module,
                     val body: Module.() -> Unit) {

    val regex = pattern.replace("?", ".").replace("*", ".*")
    fun matches(module: Module): Boolean = module.moduleName.matches(regex)
}

