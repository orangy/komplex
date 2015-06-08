
package komplex.tests.selfbuild

import org.junit.Assert
import java.io.File
import java.nio.file.Paths
import kotlin.concurrent.thread
import org.junit.Test as test

// taken from core.utils, since it is better not to link it in this test
public fun runProcess(command: Iterable<String>, listenStdout: ((String) -> Unit)? = null, listenStderr: ((String) -> Unit)? = null): Int {
    println("running: " + command.joinToString(" "))
    val process = ProcessBuilder(command.toArrayList()).redirectErrorStream(false).start()
    // read one stream in a separated thread, and another right here, to avoid java's exec deadlock
    val stderrThread = if (listenStderr != null) thread { process.getErrorStream().reader().forEachLine { listenStderr(it) } } else null
    if (listenStdout != null) process.getInputStream().reader().forEachLine { listenStdout(it) }
    process.waitFor()
    stderrThread?.join()
    return process.exitValue()
}
class selfBuildTest {

    test fun BuildKomplexTest() {
        println(Paths.get("").toAbsolutePath())
        //val javaClass.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()
        /*val classpath = listOf(
                "out/production/samples",
                "out/production/core",
                "out/production/jar",
                "out/production/kotlin",
                "out/production/javac",
                "out/production/maven",
                "lib/kotlin-compiler.jar",
                "lib/kotlin-runtime.jar",
                "lib/kotlin-reflect.jar",
                "lib/org/slf4j/slf4j-api/1.7.9/slf4j-api-1.7.9.jar",
                "lib/org/slf4j/slf4j-simple/1.7.9/slf4j-simple-1.7.9.jar"
        )*/
        val classpathstring = System.getProperty("java.class.path")
        val params = listOf("java", "-Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG", "-cp", "\"$classpathstring\"", "komplex.sample.SamplePackage")
        //val params = listOf("java", "-Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG", "-cp", classpath.joinToString(":"), "komplex.sample.SamplePackage")
        // val params = listOf("java", "-Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG", "-jar", "out/artifacts/samples_jar/samples.jar")
        val code = runProcess(params)
        Assert.assertEquals(code, 0)

    }
}