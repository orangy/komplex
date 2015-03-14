package komplex.launcher


import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.config.*
import com.intellij.openapi.util.*
import org.jetbrains.kotlin.utils.*
import org.jetbrains.kotlin.cli.jvm.*
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.arguments.*
import java.io.File
import com.sampullara.cli.Args
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.parsing.JetScriptDefinition
import java.net.*

class ScriptingHost {
    fun run(args: Array<String>) {
        val arguments = K2JVMCompilerArguments()
        arguments.freeArgs = Args.parse(arguments, args)

        val rootDisposable = Disposer.newDisposable()

        val messageCollector = MessageCollectorPlainTextToStream.PLAIN_TEXT_TO_SYSTEM_ERR
        val configuration = CompilerConfiguration()

        val paths = PathUtil.getKotlinPathsForCompiler()

        configuration.addAll(JVMConfigurationKeys.CLASSPATH_KEY, getClasspath(paths, arguments))
        //configuration.addAll(JVMConfigurationKeys.ANNOTATIONS_PATH_KEY, getAnnotationsPath(paths, arguments))
        configuration.addAll(CommonConfigurationKeys.SOURCE_ROOTS_KEY, arguments.freeArgs ?: listOf())
        configuration.put(JVMConfigurationKeys.SCRIPT_PARAMETERS, CommandLineScriptUtils.scriptParameters())
/*
        configuration.put(JVMConfigurationKeys.GENERATE_NOT_NULL_ASSERTIONS, true)
        configuration.put(JVMConfigurationKeys.GENERATE_NOT_NULL_PARAMETER_ASSERTIONS, true)
        configuration.put(JVMConfigurationKeys.ENABLE_INLINE, true)
*/
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
        configuration.put(CommonConfigurationKeys.SCRIPT_DEFINITIONS_KEY, listOf(JetScriptDefinition(".kts")))

        val environment = JetCoreEnvironment.createForProduction(rootDisposable, configuration, arrayListOf("."))
        val scriptClass = KotlinToJVMBytecodeCompiler.compileScript(configuration, paths, environment)
        if (scriptClass == null)
            return
        val instance = scriptClass.getConstructor(javaClass<Array<String>>()).newInstance(arguments.freeArgs?.copyToArray())
        rootDisposable.dispose()
    }


    private fun getClasspath(paths: KotlinPaths, arguments: K2JVMCompilerArguments): MutableList<File> {
        val classpath = arrayListOf<File>()
        classpath.addAll(PathUtil.getJdkClassesRoots())
        classpath.add(paths.getRuntimePath())
        val classPath = arguments.classpath
        if (classPath != null) {
            for (element in classPath.split(File.pathSeparatorChar)) {
                classpath.add(File(element))
            }
        }
        return classpath
    }

    private fun getAnnotationsPath(paths: KotlinPaths, arguments: K2JVMCompilerArguments): MutableList<File> {
        val annotationsPath = arrayListOf<File>()
        annotationsPath.add(paths.getJdkAnnotationsPath())
        val annotationPaths = arguments.annotations
        if (annotationPaths != null) {
            for (element in annotationPaths.split(File.pathSeparatorChar)) {
                annotationsPath.add(File(element))
            }
        }
        return annotationsPath
    }
}