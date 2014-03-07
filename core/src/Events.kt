package komplex

enum class EventStyle {
    default
    reversed
}

open class Event(val name: String, val style: EventStyle = EventStyle.default) {
    val handlers: MutableList<(Block.(Event) -> Unit)> = arrayListOf()
    fun invoke(handler: Block.(Event) -> Unit) {
        handlers.add(handler)
    }

    fun fire(block: Block) {
        val process =
                when(style) {
                    EventStyle.default -> handlers
                    EventStyle.reversed -> handlers.reverse()
                }
        for (handler in process) {
            block.handler(this)
        }
    }
}