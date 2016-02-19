
package komplex.utils

import komplex.log
import kotlin.concurrent.thread

// \todo make platform-dependent
fun escape4cli(s: String): String = if (s.contains(" ")) "\"$s\"" else s
fun escape4cli(s: Any): String = escape4cli(s.toString())

fun runProcess(command: Iterable<String>, listenStdout: (String) -> Unit = {}, listenStderr: (String) -> Unit = {}): Int {
    log.debug("running: " + command.joinToString(" "))
    val process = ProcessBuilder(command.toList()).redirectErrorStream(false).start()
    // read one stream in a separated thread, and another right here, to avoid java's exec deadlock
    val stderrThread = thread { process.errorStream.reader().forEachLine { listenStderr(it) } }
    process.inputStream.reader().forEachLine { listenStdout(it) }
    process.waitFor()
    stderrThread.join()
    return process.exitValue()
}
