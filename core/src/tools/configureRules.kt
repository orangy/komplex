package komplex.tools

import komplex.dsl.*
import komplex.utils.BuildDiagnostic
import komplex.utils.div
import java.nio.file.Path

fun<R: Rule> R.configureSingleInto(base: Path?, fn: (p: Path) -> Artifact): Boolean {
    if (targets.none())
        if (base == null) return false
        else into(fn(base))
    return true
}

private fun makeDefaultFileArtifact(module: Module, type: ArtifactType, basePath: Path, nameGen: () -> String) =
        module.file(type, basePath / nameGen())

private fun makeDefaultFolderArtifact(module: Module, type: ArtifactType, basePath: Path, nameGen: () -> String) =
        module.folder(type, basePath / nameGen())


public fun<R: Rule> R.configureSingleFolderTarget(module: komplex.dsl.Module, type: ArtifactType, nameGen: () -> String = { module.name }): BuildDiagnostic =
        if (configureSingleInto(module.env.defaultTargetDir, { makeDefaultFolderArtifact(module, type, it, nameGen) }))
            BuildDiagnostic.Success
        else BuildDiagnostic.Fail("$name (${module.fullName}) Cannot auto configure target folder: defaultTargetDir is not defined")


public fun<R: Rule> R.configureSingleTempFolderTarget(module: komplex.dsl.Module, type: ArtifactType, nameGen: () -> String = { module.name }): BuildDiagnostic =
    if (configureSingleInto(module.env.tempDir, { makeDefaultFolderArtifact(module, type, it, nameGen) }))
        BuildDiagnostic.Success
    else BuildDiagnostic.Fail("$name (${module.fullName}) Cannot auto configure target folder: tempDir is not defined")


// \todo consider using artifact type to get default extension
public fun<R: Rule> R.configureSingleFileTarget(module: komplex.dsl.Module, type: ArtifactType, nameGen: () -> String = { module.name }): BuildDiagnostic =
    if (configureSingleInto(module.env.defaultTargetDir, { makeDefaultFileArtifact(module, type, it, nameGen) }))
        BuildDiagnostic.Success
    else BuildDiagnostic.Fail("$name (${module.fullName}) Cannot auto configure target folder: defaultTargetDir is not defined")


public fun<R: Rule> R.configureSingleTempFileTarget(module: komplex.dsl.Module, type: ArtifactType, nameGen: () -> String = { module.name }): BuildDiagnostic =
        if (configureSingleInto(module.env.tempDir, { makeDefaultFileArtifact(module, type, it, nameGen) }))
            BuildDiagnostic.Success
        else BuildDiagnostic.Fail("$name (${module.fullName}) Cannot auto configure target folder: tempDir is not defined")
