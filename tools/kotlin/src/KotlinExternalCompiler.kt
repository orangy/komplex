package komplex.tools.kotlin

import komplex.dsl.FolderArtifact
import komplex.utils.escape4cli
import komplex.utils.runProcess
import java.io.File
import java.nio.file.Path

fun komplex.dsl.tools.kotlin(compilerCmd: Iterable<String>): KotlinCompilerRule =
        KotlinCompilerRule(komplex.model.LazyTool<KotlinCompilerRule, KotlinExternalCompiler>("Kotlin compiler", { KotlinExternalCompiler(compilerCmd) } ))

fun komplex.dsl.tools.kotlin(vararg compilerCmd: String): KotlinCompilerRule = komplex.dsl.tools.kotlin(compilerCmd.asIterable())

fun komplex.dsl.tools.kotlin(compilerJarPath: Path): KotlinCompilerRule = komplex.dsl.tools.kotlin("java", "-jar", compilerJarPath.toString())


class KotlinExternalCompiler(val compilerCmd: Iterable<String>) : KotlinCompiler() {
    override val name: String = "Kotlin external compiler"

    override fun compile(destFolder: FolderArtifact, kotlinSources: Iterable<Path>, sourceRoots: Iterable<String>, libraries: Iterable<Path>, includeRuntime: Boolean): komplex.utils.BuildDiagnostic {

        val destFolderFile = destFolder.path.toFile()
        if (!destFolderFile.exists())
            destFolderFile.mkdirs()

        val ktccmdline: MutableList<String> = compilerCmd.map { escape4cli(it) }.toMutableList()

        ktccmdline.addAll(listOf("-d", escape4cli(destFolder.path.toString())))

        if (libraries.any()) ktccmdline.addAll(listOf( "-classpath", "${escape4cli(libraries.joinToString(File.pathSeparator))}"))

        if (!includeRuntime) ktccmdline.add("-no-stdlib")

        ktccmdline.addAll(kotlinSources.map { escape4cli(it.toString()) })
        // note: this is not a good way to pass java source roots, since it limits control over partial compilation
        // \todo - promote idea of adding special option to compiler and use it
        ktccmdline.addAll(sourceRoots.map { escape4cli(it.toString()) })

        val res = runProcess(ktccmdline, { log.info(it) }, { log.info(it) })

        return if (res == 0) komplex.utils.BuildDiagnostic.Success
               else komplex.utils.BuildDiagnostic.Fail
    }
}
