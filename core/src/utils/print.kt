
package komplex.utils

public interface IndentLn {
    public fun inc(count: Int = 1): IndentLn
    public override fun toString(): String
}

public interface NicePrintable {
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


// converted from http://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java

val hexArray: CharArray = "0123456789ABCDEF".toCharArray()

public fun ByteArray.toHexString(): String {
    val hexChars = CharArray(size() * 2)
    for (j in indices) {
        val v = this[j].toInt() and 255
        hexChars[j * 2] = hexArray[v.ushr(4)]
        hexChars[j * 2 + 1] = hexArray[v and 15]
    }
    return String(hexChars)
}

