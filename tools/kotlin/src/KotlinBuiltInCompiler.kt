package komplex.tools.kotlin

import com.intellij.openapi.util.Disposer
import komplex.dsl.FolderArtifact
import komplex.utils
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.addKotlinSourceRoot
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.nio.file.Path


public val komplex.dsl.tools.kotlin: KotlinCompilerRule
    get() = KotlinCompilerRule(komplex.model.LazyTool<KotlinCompilerRule, KotlinBuiltInCompiler>("Kotlin compiler", { KotlinBuiltInCompiler() } ))

public class KotlinBuiltInCompiler() : KotlinCompiler() {
    override val name: String = "Kotlin built-in compiler"

    override fun compile(destFolder: FolderArtifact, kotlinSources: Iterable<Path>, sourceRoots: Iterable<String>, libraries: Iterable<Path>, includeRuntime: Boolean): utils.BuildDiagnostic {

        val messageCollector = object : MessageCollector {
            override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation) {
                fun msg() = if (location == CompilerMessageLocation.NO_LOCATION) "$message" else "$message ($location)"
                when (severity) {
                    in CompilerMessageSeverity.ERRORS -> log.error("Error: " + msg())
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

    private fun runCompiler(compilerCfg: CompilerConfiguration, destFolder: FolderArtifact, messageCollector: MessageCollector, includeRuntime: Boolean): utils.BuildDiagnostic {

        val rootDisposable = Disposer.newDisposable()

        val destFolderFile = destFolder.path.toFile()
        if (!destFolderFile.exists())
            destFolderFile.mkdirs()

        try {
            val environment = KotlinCoreEnvironment.createForProduction(rootDisposable, compilerCfg, EnvironmentConfigFiles.JVM_CONFIG_FILES)
            if (KotlinToJVMBytecodeCompiler.compileBunchOfSources(environment, null, destFolderFile, includeRuntime))
                return utils.BuildDiagnostic.Success
            else
                // \todo add errors into diagnostics
                return utils.BuildDiagnostic.Fail("Compilation error")
        } catch (e: CompilationException) {
            messageCollector.report(CompilerMessageSeverity.EXCEPTION, OutputMessageUtil.renderException(e),
                    MessageUtil.psiElementToMessageLocation(e.getElement()))
            return utils.BuildDiagnostic.Fail("Internal error: ${OutputMessageUtil.renderException(e)}")
        } finally {
            rootDisposable.dispose()
        }
    }
}
