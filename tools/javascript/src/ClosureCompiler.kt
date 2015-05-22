package komplex.tools.javascript

import com.google.javascript.jscomp.*
import com.google.javascript.jscomp.Compiler
import komplex.model.ArtifactDesc
import komplex.model.Tool
import komplex.model
import komplex.dsl
import komplex.tools
import komplex.data
import komplex.data.openFileSet
import komplex.data.openInputStream
import komplex.data.openOutputStream
import komplex.dsl.*
import komplex.model.ArtifactData
import komplex.utils
import komplex.tools.filterIn
import komplex.tools.getPaths
import komplex.utils.LogLevel
import komplex.utils.LogOutputStream
import komplex.utils.escape4cli
import org.slf4j.LoggerFactory
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.util.*

val log = LoggerFactory.getLogger("komplex.tools.js")

public val komplex.dsl.tools.closureCompiler: ClosureCompilerRule
    get() = ClosureCompilerRule(komplex.model.LazyTool<ClosureCompilerRule, ClosureCompilerTool>("closure javascript compiler", { ClosureCompilerTool() } ))

public class ClosureCompilerRule(tool: Tool<ClosureCompilerRule>) : komplex.dsl.BasicToolRule<ClosureCompilerRule, komplex.model.Tool<ClosureCompilerRule>>(tool) {

    public val explicitExterns: RuleSources = RuleSources()

    public val externSources: Iterable<ArtifactDesc> get() = explicitExterns.collect(selector.scenarios)

    override val sources: Iterable<ArtifactDesc> get() = super.sources + externSources
}

public fun <S: GenericSourceType> ClosureCompilerRule.extern(args: Iterable<S>): ClosureCompilerRule = addToSources(explicitExterns, args)
public fun <S: GenericSourceType> ClosureCompilerRule.extern(vararg args: S): ClosureCompilerRule = addToSources(explicitExterns, *args)


public class ClosureCompilerTool() : komplex.model.Tool<ClosureCompilerRule> {

    override val name: String = "closure javascript compiler"

    override fun execute(context: model.BuildContext,
                         cfg: ClosureCompilerRule,
                         src: Iterable<Pair<model.ArtifactDesc, model.ArtifactData?>>,
                         tgt: Iterable<model.ArtifactDesc>
    ): model.BuildResult {

        val project = context.module

        val kotlinSources = src.filterIn(cfg.fromSources).getPaths()

        if (kotlinSources.none()) {
            log.error("Error: No sources to compile in module ${project.name}: ${src.map { it.first }}")
            return model.BuildResult(utils.BuildDiagnostic.Fail)
        }

        // first is target second is meta
        val dest = tgt.single() as? FileArtifact ?: throw Exception("Expecting file artifact as a target for tool $name")

// \todo implement it and reuse for error handling
//        val errorManager = object : ErrorManager { }

        val compiler = Compiler(PrintStream(LogOutputStream(log, LogLevel.INFO)))
        val options = CompilerOptions()
        CompilationLevel.WHITESPACE_ONLY.setOptionsForCompilationLevel(options)
        options.setLanguageIn(CompilerOptions.LanguageMode.ECMASCRIPT5_STRICT)

        fun Iterable<Pair<model.ArtifactDesc, model.ArtifactData?>>.openSourceStreams(): List<SourceFile> =
            flatMap { openFileSet(it).coll }.map { SourceFile.fromInputStream(it.path.toString(), openInputStream(it).inputStream, StandardCharsets.UTF_8) }

        val res = compiler.compile(
                src.filterIn(cfg.externSources).openSourceStreams(),
                src.filterIn(cfg.fromSources).openSourceStreams(),
                options)

        if (res.success) {
            val streamData = openOutputStream(dest)
            streamData.outputStream.writer().write(compiler.toSource())
            return model.BuildResult(utils.BuildDiagnostic.Success, listOf(Pair(dest, streamData)))
        }
        else return model.BuildResult(utils.BuildDiagnostic.Fail(res.errors.map { "Error in '${it.sourceName}' (${it.lineNumber}): ${it.description}" }))
    }
}
