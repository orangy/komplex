package komplex


fun config(name : String, body : Config.()->Unit = {}) : Config {
    val configuration = Config(name)
    configuration.body()
    return configuration
}

class Config(val pattern: String) {
    val regex = pattern.replace("?",".").replace("*", ".*")

    fun matches(config: String) : Boolean = config.matches(regex)

    override fun toString(): String {
        return pattern
    }
}