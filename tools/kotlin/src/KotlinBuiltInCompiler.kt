package komplex.tools.kotlin

import com.intellij.openapi.util.*
import komplex.dsl.*
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.config.*
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.utils.*
import java.io.*
import java.nio.file.*


val komplex.dsl.tools.kotlin: KotlinCompilerRule
    get() = KotlinCompilerRule(komplex.model.LazyTool<KotlinCompilerRule, KotlinBuiltInCompiler>("Kotlin compiler", { KotlinBuiltInCompiler() }))

class KotlinBuiltInCompiler() : KotlinCompiler() {
    override val name: String = "Kotlin built-in compiler"

    override fun compile(destFolder: FolderArtifact, kotlinSources: Iterable<Path>, sourceRoots: Iterable<String>, libraries: Iterable<Path>, includeRuntime: Boolean): komplex.utils.BuildDiagnostic {

        val messageCollector = object : MessageCollector {
            var hasErrors = false

            override fun clear() {}

            override fun hasErrors(): Boolean {
                return hasErrors
            }

            override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation) {
                fun msg() = if (location == CompilerMessageLocation.NO_LOCATION) "$message" else "$message ($location)"
                when (severity) {
                    in CompilerMessageSeverity.ERRORS -> {
                        hasErrors = true
                        log.error("Error: " + msg())
                    }
                    CompilerMessageSeverity.LOGGING -> log.debug(msg())
                    CompilerMessageSeverity.INFO -> log.info(msg())
                    CompilerMessageSeverity.WARNING -> log.info("Warning: " + msg())
                //CompilerMessageSeverity.OUTPUT -> log.trace(msg())
                }
            }
        }

        val compilerCfg = CompilerConfiguration()

        kotlinSources.forEach { compilerCfg.addKotlinSourceRoot(it.toString()) }
        sourceRoots.forEach { compilerCfg.addJavaSourceRoot(File(it)) }
        compilerCfg.addJvmClasspathRoots(libraries.map { it.toFile() })
        compilerCfg.addJvmClasspathRoots(PathUtil.getJdkClassesRoots())
        compilerCfg.put<MessageCollector>(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)

        // \todo extract actually compiled class files

        return runCompiler(compilerCfg, destFolder, messageCollector, includeRuntime)
    }

    private fun runCompiler(compilerCfg: CompilerConfiguration, destFolder: FolderArtifact, messageCollector: MessageCollector, includeRuntime: Boolean): komplex.utils.BuildDiagnostic {

        val rootDisposable = Disposer.newDisposable()

        val destFolderFile = destFolder.path.toFile()
        if (!destFolderFile.exists())
            destFolderFile.mkdirs()

        try {
            val environment = KotlinCoreEnvironment.createForProduction(rootDisposable, compilerCfg, EnvironmentConfigFiles.JVM_CONFIG_FILES)
            val friendPaths = listOf<String>()
            if (KotlinToJVMBytecodeCompiler.compileBunchOfSources(environment/*, null, destFolderFile, friendPaths, includeRuntime*/))
                return komplex.utils.BuildDiagnostic.Success
            else
            // \todo add errors into diagnostics
                return komplex.utils.BuildDiagnostic.Fail("Compilation error")
        } catch (e: CompilationException) {
            messageCollector.report(CompilerMessageSeverity.EXCEPTION, OutputMessageUtil.renderException(e),
                    MessageUtil.psiElementToMessageLocation(e.element))
            return komplex.utils.BuildDiagnostic.Fail("Internal error: ${OutputMessageUtil.renderException(e)}")
        } finally {
            rootDisposable.dispose()
        }
    }
}
