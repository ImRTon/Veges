package tw.taipei.veges.releasetool

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import tw.taipei.veges.releasetool.artifact.ArtifactCodec
import tw.taipei.veges.releasetool.artifact.SourceSnapshotArtifact
import tw.taipei.veges.releasetool.artifact.TaxonomyArtifact
import tw.taipei.veges.releasetool.audit.AuditOptions
import tw.taipei.veges.releasetool.audit.CatalogAuditRunner
import tw.taipei.veges.releasetool.audit.toDocument
import tw.taipei.veges.releasetool.model.HistoricalAuditRunner
import tw.taipei.veges.releasetool.model.RetailCalibrationMappingsArtifact

fun main(args: Array<String>) {
    if (args.firstOrNull() == "checksum-taxonomy") {
        val options = parseOptions(args.drop(1))
        val taxonomy = readJson<TaxonomyArtifact>(options.required("taxonomy"))
        println(ArtifactCodec.taxonomyChecksum(taxonomy))
        return
    }
    if (args.firstOrNull() == "checksum-snapshot") {
        val options = parseOptions(args.drop(1))
        val snapshot = readJson<SourceSnapshotArtifact>(options.required("snapshot"))
        println(ArtifactCodec.snapshotChecksum(snapshot))
        return
    }
    if (args.firstOrNull() == "audit-history") {
        val options = parseOptions(args.drop(1))
        val taxonomy = readJson<TaxonomyArtifact>(options.required("taxonomy"))
        val mappings = readJson<RetailCalibrationMappingsArtifact>(options.required("retail-mappings"))
        val wholesaleDirectory = Paths.get(options.required("wholesale-dir"))
        val retailDirectory = Paths.get(options.required("retail-dir"))
        val report = HistoricalAuditRunner().run(
            taxonomy = taxonomy,
            mappings = mappings,
            wholesaleFiles = Files.list(wholesaleDirectory).use { stream ->
                stream.filter { it.toString().endsWith(".json") }.sorted().toList()
            },
            retailFiles = Files.list(retailDirectory).use { stream ->
                stream.filter { it.toString().endsWith(".csv") }.sorted().toList()
            },
            generatedAt = options.required("generated-at"),
        )
        writeJson(Paths.get(options.required("output")), report)
        println("Wrote historical audit with ${report.result.size} concept/basis rows")
        return
    }
    if (args.firstOrNull() != "audit-taxonomy") {
        printUsage()
        return
    }

    val options = parseOptions(args.drop(1))
    val taxonomy = readJson<TaxonomyArtifact>(options.required("taxonomy"))
    val snapshot = readJson<SourceSnapshotArtifact>(options.required("snapshot"))
    val audit = CatalogAuditRunner().audit(
        taxonomy = taxonomy,
        snapshot = snapshot,
        options = AuditOptions(generatedAt = options.required("generated-at")),
    ).toDocument()

    val outputDirectory = Paths.get(options.required("output-dir"))
    Files.createDirectories(outputDirectory)
    writeJson(outputDirectory.resolve("catalog-audit.json"), audit)
    writeJson(outputDirectory.resolve("exclusions.json"), audit.exclusions)
    println("Wrote ${audit.metrics.size} concept metrics and ${audit.exclusions.size} exclusions")
}

private data class CliOptions(private val values: Map<String, String>) {
    fun required(name: String): String = requireNotNull(values[name]) {
        "Missing --$name. Use audit-taxonomy --help for usage."
    }
}

private fun parseOptions(arguments: List<String>): CliOptions {
    if (arguments.firstOrNull() == "--help") {
        printUsage()
        return CliOptions(emptyMap())
    }
    val values = arguments.chunked(2).associate { pair ->
        require(pair.size == 2 && pair[0].startsWith("--")) {
            "Arguments must use --name value pairs"
        }
        pair[0].removePrefix("--") to pair[1]
    }
    return CliOptions(values)
}

private inline fun <reified T> readJson(path: String): T =
    ArtifactCodec.strictJson.decodeFromString(Files.readString(Paths.get(path), StandardCharsets.UTF_8))

private inline fun <reified T> writeJson(path: Path, value: T) {
    Files.writeString(
        path,
        ArtifactCodec.strictJson.encodeToString(value) + "\n",
        StandardCharsets.UTF_8,
    )
}

private fun printUsage() {
    println(
        """
        Veges release tool

        checksum-taxonomy --taxonomy <path>
        checksum-snapshot --snapshot <path>
        audit-history --taxonomy <path> --retail-mappings <path> --wholesale-dir <path> --retail-dir <path> --generated-at <ISO instant> --output <path>
        audit-taxonomy --taxonomy <path> --snapshot <path> --generated-at <ISO instant> --output-dir <path>
        """.trimIndent(),
    )
}
