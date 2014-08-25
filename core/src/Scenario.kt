package komplex


public fun config(name : String, body : Scenario.()->Unit = {}) : Scenario {
    val configuration = Scenario(name)
    configuration.body()
    return configuration
}

public class Scenario(val pattern: String) {
    val regex = pattern.replace("?",".").replace("*", ".*")

    fun matches(config: String) : Boolean = config.matches(regex)

    override fun toString(): String {
        return pattern
    }
}