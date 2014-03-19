package komplex.launcher

fun main(args: Array<String>) {
    ScriptingHost().run(args)
}

class Script2(context : String) {
    val name = "foo"
    val len : Int;
    {
        len = name.length
    }

    val x = len * 2
    val y : Int;
    {
        y = x - 1
    }
}