package komplex.dependencies

import java.nio.file.Path
import komplex.*
import java.nio.file.Files


public object resolver {
    public fun resolve(reference: Reference, scenario: Scenario): List<Path> =
        when (reference) {
            is LibraryReference -> repositories.resolve(reference)
            is ModuleReference -> resolveModule(reference, scenario)
            else -> throw Exception("unknown reference type")
        }

    fun resolveModule(reference: ModuleReference, scenario: Scenario): List<Path> {
        var res = arrayListOf<Path>()
        for (ruleSet in reference.module.build.ruleSets)
            if (ruleSet.selectors.any { it.matches(scenario) })
                for (rule in ruleSet.rules)
                    for (dest in rule.targets())
                        if (dest is FolderArtifact && Files.exists(dest.path)) res.add(dest.path)
                        else if (dest is FileArtifact && Files.exists(dest.path)) res.add(dest.path)
        return res
    }
}
