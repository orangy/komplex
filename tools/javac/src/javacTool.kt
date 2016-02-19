package komplex.tools.javac

import komplex.data.OpenFileSet
import komplex.model.Tool
import komplex.tools.filterIn
import komplex.tools.getPaths
import komplex.utils.escape4cli
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import javax.tools.*


val komplex.dsl.tools.javac: JavaCompilerRule
    get() = JavaCompilerRule(komplex.model.LazyTool<JavaCompilerRule, JavaCompiler>("Kotlin compiler", { JavaCompiler()} ))

val log = LoggerFactory.getLogger("komplex.tools.javac")

// separate class for separate class loading
// \todo check if moving to separate file or jar is needed for really lazy tool loading, or may be that nested class will work as well
class JavaCompilerRule(override val tool: Tool<JavaCompilerRule>) : komplex.tools.JVMCompilerRule<JavaCompilerRule, Tool<JavaCompilerRule>>() {
    override val name: String = "Java compiler"
    override val config: JavaCompilerRule = this

    fun invoke(body: JavaCompilerRule.() -> Unit): JavaCompilerRule {
        body()
        return this
    }
    var enableInline: Boolean = true
}


fun JavaSource(path: String) = JavaSource(File(path))

class JavaSource(val file: File) : SimpleJavaFileObject(file.toURI(), JavaFileObject.Kind.SOURCE) {
    override fun openInputStream(): InputStream? = file.inputStream()

    override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence? =
        openInputStream()!!.bufferedReader().readText()
}

class JavaCompiler() : komplex.model.Tool<JavaCompilerRule> {
    override val name: String = "Java compiler"

    //    override fun compile(context: BuildStepContext, from: Iterable<Artifact>, to: Iterable<Artifact>, useLibs: Iterable<Artifact>): BuildResult = null!!

    override fun execute(context: komplex.model.BuildContext,
                         cfg: JavaCompilerRule,
                         src: Iterable<Pair<komplex.model.ArtifactDesc, komplex.model.ArtifactData?>>,
                         tgt: Iterable<komplex.model.ArtifactDesc>
    ): komplex.model.BuildResult {
        val libraries = src.filterIn(cfg.classpathSources).getPaths(OpenFileSet.FoldersAsLibraries).distinct()

        val folder = tgt.single()
        val targetFolder =
                when (folder) {
                    is komplex.dsl.FolderArtifact -> {
                        val dir = folder.path.toFile()
                        if (!dir.exists())
                            dir.mkdirs()
                        folder.path.toString()
                    }
                    else -> throw IllegalArgumentException("Compiler only supports single folder as destination")
                }

        val options = arrayListOf("-cp", libraries.joinToString(File.pathSeparator), "-d", targetFolder)

        val compiler = ToolProvider.getSystemJavaCompiler()
        val diagnostics = DiagnosticCollector<JavaFileObject>()
        val fileManager=compiler.getStandardFileManager(diagnostics, null, null)

        val sources = src.filterIn(cfg.fromSources).getPaths()

        log.debug("javac options: ${options.joinToString(" ")}")
        log.debug("sources: ${sources.map { escape4cli(it) }.joinToString("\n  ","\n  ")}")

        val task=compiler.getTask(null,fileManager,diagnostics,options,null, sources.map { JavaSource(it.toFile()) })

        log.info("start compilation")

        val success = task.call()

        log.info("compilation finished. diagnostics:")

        for (diag in diagnostics.diagnostics) {
            val msg = "${diag.source?.name ?: "<no source>"} (${diag.lineNumber},${diag.columnNumber}) ${diag.getMessage(null)}"
            when (diag.kind) {
                Diagnostic.Kind.ERROR -> log.error(msg)
                Diagnostic.Kind.NOTE -> log.debug(msg)
                Diagnostic.Kind.MANDATORY_WARNING -> log.info(msg)
                Diagnostic.Kind.WARNING -> log.info(msg)
            //Diagnostic.Kind.OTHER -> log.trace(msg)
            }
        }

        return if (success)
            komplex.model.BuildResult(komplex.utils.BuildDiagnostic.Success, listOf(Pair(folder, komplex.data.openFileSet(folder))))
            else komplex.model.BuildResult(komplex.utils.BuildDiagnostic.Fail)
    }
}

