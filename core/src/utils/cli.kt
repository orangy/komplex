
package komplex.utils

import komplex.log
import kotlin.concurrent.thread

// \todo make platform-dependent
public fun escape4cli(s: String): String = if (s.contains(" ")) "\"$s\"" else s

public fun runProcess(command: Collection<String>, listenStdout: (String) -> Unit = {}, listenStderr: (String) -> Unit = {}): Int {
    log.debug("running: " + command.joinToString(" "))
    val process = ProcessBuilder(command.toArrayList()).redirectErrorStream(false).start()
    // read one stream in a separated thread, and another right here, to avoid java's exec deadlock
    val stderrThread = thread { process.getErrorStream().reader().forEachLine { listenStderr(it) } }
    process.getInputStream().reader().forEachLine { listenStdout(it) }
    process.waitFor()
    stderrThread.join()
    return process.exitValue()
}
