
package komplex.model

import komplex.*
import komplex.data.*
import komplex.utils.BuildDiagnostic
import komplex.utils.toHexString

public open class GraphBuildContext(val baseScenario: Scenarios,
                                    public val graph: BuildGraph,
                                    public val sourceHashes: MutableMap<String, ByteArray> = hashMapOf(),
                                    public val detailedHashes: MutableMap<String, ByteArray?>? = null
) : BuildContext {
    public var node: BuildGraphNode? = null
    override val module: Module
        get() = node!!.moduleFlavor.module
    override val scenario = node?.moduleFlavor?.scenarios?.resolve(baseScenario) ?: baseScenario
    public val artifacts: MutableMap<ArtifactDesc, ArtifactData?> = hashMapOf()
}


public fun BuildGraph.build(scenario: Scenarios,
                            context: GraphBuildContext = GraphBuildContext(scenario, this),
                            sources: Set<BuildGraphNode> = this.roots(scenario).toHashSet(),
                            targets: Iterable<BuildGraphNode> = this.leafs(scenario))
{
    fun buildNode(n: BuildGraphNode): Boolean {
        val artifacts = hashMapOf<ArtifactDesc, ArtifactData?>()
        // \todo consider better way to detect uncasheable steps/artefacts and steps that handle cashing themselves, e.g. maven
        // e.g. it could be done inside the steps or tools themselves
        var hasNonHasheableArtifact = false
        for (it in n.step.sources) {
            // \todo: MT-safety!
            // \todo eliminate artifact data opening in steps if they already opened here, only conversion may be applied in cases, e.g. file to stream
            val data = context.artifacts.getOrPut(it, { openDataSet(it) })
            artifacts.set(it, data)
            // \todo - this is cumbersome, consider more reliable approach and see more generic todo about detection of skippable steps above
            if (!hasNonHasheableArtifact && (data is DataSet<*> && data.coll.any { it is DummyData } || data is DummyData)) {
                log.debug("non-cacheable artifact on step: ${n.step.name} in module ${n.moduleFlavor.module.name}: $it")
                hasNonHasheableArtifact = true
            }
        }
        val hash = mergeHashes(artifacts.values())
        if (!hasNonHasheableArtifact && n.step.targets.all { context.sourceHashes.get(it.name)?.hashEquals(hash) ?: false }) {
            log.info("skipping: already built: ${n.step.name} in module ${n.moduleFlavor.module.name}")
            return false
        } else {
            log.info("running ${n.step.name} in module ${n.moduleFlavor.module.name}")
            if (context.detailedHashes != null) {
                log.trace("Hashes mismatch for the following sources (actual vs expected):")
                for (it in artifacts) {
                    val name = it.getKey().name
                    val actual = it.getValue()
                    val expected = context.detailedHashes.get(name)
                    fun hashStr(hash: ByteArray?, valName: String) = when { hash == null -> "null"; hash.size() == 0 -> "[]"; else -> "[${hash.toHexString()}]" }
                    if (log.isTraceEnabled() && (actual == null || expected == null || !actual.hash.hashEquals(expected)))
                        log.trace("    $name: ${hashStr(actual?.hash, "x")} != ${hashStr(expected, "y")}")
                    context.detailedHashes?.put(name, actual?.hash)
                }
            }
            context.node = n
            val result = n.step.execute(context, artifacts)
            result.result.forEach { it -> context.artifacts.set(it.first, it.second) }
            n.step.targets.forEach { context.sourceHashes.put(it.name, hash) }
            if (result.diagnostic == BuildDiagnostic.Success) {
                log.info("${n.step.name} succeeded!")
                result.diagnostic.messages.forEach { log.warn("${n.step.name}: $it") }
            } else {
                log.info("${n.step.name} failed!")
                result.diagnostic.messages.forEach { log.error("${n.step.name}: $it") }
            }

            return result.diagnostic != BuildDiagnostic.Success
        }
    }

    buildPartialApply( scenario, ::buildNode, sources, targets)
}


