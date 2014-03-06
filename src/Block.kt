package komplex

fun block(body : Block.()->Unit) : Block {
    val block = Block()
    block.body()
    return block
}


class Block() : Project("<block>", null) {
    fun resolve() {

    }

    fun dump() {
        dump(this)
    }
}