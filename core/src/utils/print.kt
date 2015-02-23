
package komplex.utils

public trait IndentLn {
    public fun inc(count: Int = 1): IndentLn
    public override fun toString(): String
}

public trait NicePrintable {
    public fun nicePrint(indent: IndentLn): String
}


public class SpaceIndent(val shift: Int = 1) : IndentLn {
    override fun toString(): String = " ".repeat(shift)
    override fun inc(count: Int): IndentLn = this
}

public class TwoSpaceIndentLn(val shift: Int = 0) : IndentLn {
    override fun toString(): String = "\n" + "  ".repeat(shift)
    override fun inc(count: Int): IndentLn = TwoSpaceIndentLn(shift + count)
}
