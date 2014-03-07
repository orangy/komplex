package komplex

public fun block(body : Block.()->Unit) : Block {
    val block = Block()
    block.body()
    return block
}


public class Block() : Project("<block>", null) {
    fun resolve() {

    }

    fun dump() {
        dump(this)
    }
}