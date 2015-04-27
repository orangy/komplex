
package komplex.utils

// \todo make platform-dependent
public fun escape4cli(s: String): String = if (s.contains(" ")) "\"$s\"" else s
