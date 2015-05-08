package komplex.tools.kotlin

import komplex.dsl.FolderArtifact
import komplex.utils
import komplex.tools.kotlin.KotlinCompiler
import komplex.tools.kotlin.KotlinCompilerRule
import komplex.tools.kotlin.log
import komplex.utils.escape4cli
import komplex.utils.runProcess
import java.io.File
import java.nio.file.Path

public fun komplex.dsl.tools.kotlin(compilerCmd: String): KotlinCompilerRule =
        KotlinCompilerRule(komplex.model.LazyTool<KotlinCompilerRule, KotlinExternalCompiler>("Kotlin compiler", { KotlinExternalCompiler(compilerCmd) } ))

public class KotlinExternalCompiler(val compilerCmd: String) : KotlinCompiler() {
    override val name: String = "Kotlin external compiler"

    override fun compile(destFolder: FolderArtifact, kotlinSources: Iterable<Path>, sourceRoots: Iterable<String>, libraries: Iterable<Path>, includeRuntime: Boolean): utils.BuildDiagnostic {

        val destFolderFile = destFolder.path.toFile()
        if (!destFolderFile.exists())
            destFolderFile.mkdirs()

        val ktccmdline = arrayListOf(
                escape4cli(compilerCmd),
                "-d",
                escape4cli(destFolder.path.toString()),
                "-classpath",
                "${escape4cli(libraries.joinToString(File.pathSeparator))}")

        if (!includeRuntime) ktccmdline.add("-no-stdlib")

        ktccmdline.addAll(kotlinSources.map { escape4cli(it.toString()) })
        // note: this is not a good way to pass java source roots, since it limits control over partial compilation
        // \todo - promote idea of adding special option to compiler and use it
        ktccmdline.addAll(sourceRoots.map { escape4cli(it.toString()) })

        val res = runProcess(ktccmdline, { log.info(it) }, { log.error(it) })

        return if (res == 0) utils.BuildDiagnostic.Success
               else utils.BuildDiagnostic.Fail
    }
}
