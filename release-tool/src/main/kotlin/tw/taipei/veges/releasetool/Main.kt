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
import tw.taipei.veges.releasetool.artifact.VegetableCatalogArtifact
import tw.taipei.veges.releasetool.audit.AuditOptions
import tw.taipei.veges.releasetool.audit.CatalogAuditRunner
import tw.taipei.veges.releasetool.audit.toDocument
import tw.taipei.veges.releasetool.catalog.ExhaustiveVegetableTaxonomyGenerator
import tw.taipei.veges.releasetool.catalog.ApprovedTaxonomyFinalizer
import tw.taipei.veges.releasetool.model.HistoricalAuditRunner
import tw.taipei.veges.releasetool.model.HistoricalAuditReport
import tw.taipei.veges.releasetool.model.EstimationPublicationPolicy
import tw.taipei.veges.releasetool.model.ModelArtifactBundle
import tw.taipei.veges.releasetool.model.ModelArtifactGenerator
import tw.taipei.veges.releasetool.model.RetailCalibrationMappingsArtifact
import tw.taipei.veges.releasetool.model.TemporaryFactorArtifact
import tw.taipei.veges.releasetool.model.TemporaryFactorArtifactGenerator
import tw.taipei.veges.releasetool.model.TemporaryFactorPolicy

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
    if (args.firstOrNull() == "checksum-model-artifact") {
        val options = parseOptions(args.drop(1))
        val artifact = readJson<ModelArtifactBundle>(options.required("artifact"))
        println(ArtifactCodec.modelArtifactChecksum(artifact))
        return
    }
    if (args.firstOrNull() == "validate-model-artifact") {
        val options = parseOptions(args.drop(1))
        val artifact = readJson<ModelArtifactBundle>(options.required("artifact"))
        ModelArtifactGenerator.validate(artifact)
        println("Model artifact is valid")
        return
    }
    if (args.firstOrNull() == "checksum-temporary-factor-artifact") {
        val options = parseOptions(args.drop(1))
        val artifact = readJson<TemporaryFactorArtifact>(options.required("artifact"))
        println(ArtifactCodec.temporaryFactorArtifactChecksum(artifact))
        return
    }
    if (args.firstOrNull() == "validate-temporary-factor-artifact") {
        val options = parseOptions(args.drop(1))
        val artifact = readJson<TemporaryFactorArtifact>(options.required("artifact"))
        TemporaryFactorArtifactGenerator.validate(artifact)
        println("Temporary factor artifact is valid")
        return
    }
    if (args.firstOrNull() == "generate-temporary-factor-artifact") {
        val options = parseOptions(args.drop(1))
        val policy = readJson<TemporaryFactorPolicy>(options.required("policy"))
        val artifact = TemporaryFactorArtifactGenerator.generate(
            policy = policy,
            artifactVersion = options.required("artifact-version"),
            generatedAt = options.required("generated-at"),
        )
        TemporaryFactorArtifactGenerator.validate(artifact)
        writeJson(Paths.get(options.required("output")), artifact)
        println("Wrote temporary factor artifact ${artifact.artifactVersion}")
        return
    }
    if (args.firstOrNull() == "generate-exhaustive-vegetable-taxonomy") {
        val options = parseOptions(args.drop(1))
        val base = readJson<TaxonomyArtifact>(options.required("base-taxonomy"))
        val inventory = readJson<VegetableCatalogArtifact>(options.required("vegetable-inventory"))
        val artifact = ExhaustiveVegetableTaxonomyGenerator.generate(
            base = base,
            inventory = inventory,
            taxonomyVersion = options.required("taxonomy-version"),
            reviewedAt = options.required("reviewed-at"),
            reviewedBy = options.required("reviewed-by"),
        )
        writeJson(Paths.get(options.required("output")), artifact)
        println(
            "Wrote ${artifact.concepts.count { it.category.name == "VEGETABLE" }} exhaustive vegetable varieties " +
                "and ${artifact.concepts.count { it.category.name == "FRUIT" }} reviewed fruit concepts",
        )
        return
    }
    if (args.firstOrNull() == "finalize-approved-taxonomy") {
        val options = parseOptions(args.drop(1))
        val candidate = readJson<TaxonomyArtifact>(options.required("candidate-taxonomy"))
        val auditRaw = Files.readString(Paths.get(options.required("illustration-audit")), StandardCharsets.UTF_8)
        val artifact = ApprovedTaxonomyFinalizer.finalize(
            candidate = candidate,
            illustrationAudit = ApprovedTaxonomyFinalizer.decodeAudit(auditRaw),
            taxonomyVersion = options.required("taxonomy-version"),
            approvedAt = options.required("approved-at"),
            approvedBy = options.required("approved-by"),
        )
        writeJson(Paths.get(options.required("output")), artifact)
        println(
            "Wrote approved launch taxonomy with ${artifact.concepts.size} concepts " +
                "and checksum ${artifact.artifactChecksum}",
        )
        return
    }
    if (args.firstOrNull() == "audit-history") {
        val options = parseOptions(args.drop(1))
        val taxonomy = readJson<TaxonomyArtifact>(options.required("taxonomy"))
        val mappings = readJson<RetailCalibrationMappingsArtifact>(options.required("retail-mappings"))
        val policy = readJson<EstimationPublicationPolicy>(options.required("policy"))
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
            policy = policy,
        )
        writeJson(Paths.get(options.required("output")), report)
        println("Wrote historical audit with ${report.result.size} concept/basis rows")
        return
    }
    if (args.firstOrNull() == "generate-model-artifact") {
        val options = parseOptions(args.drop(1))
        val report = readJson<HistoricalAuditReport>(options.required("audit"))
        val policy = readJson<EstimationPublicationPolicy>(options.required("policy"))
        val artifact = ModelArtifactGenerator.generate(
            report = report,
            policy = policy,
            artifactVersion = options.required("artifact-version"),
        )
        ModelArtifactGenerator.validate(artifact)
        writeJson(Paths.get(options.required("output")), artifact)
        println("Wrote ${artifact.entries.size} model entries and ${artifact.exclusions.size} exclusions")
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
    path.parent?.let(Files::createDirectories)
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
        checksum-model-artifact --artifact <path>
        validate-model-artifact --artifact <path>
        checksum-temporary-factor-artifact --artifact <path>
        validate-temporary-factor-artifact --artifact <path>
        generate-temporary-factor-artifact --policy <path> --artifact-version <version> --generated-at <ISO instant> --output <path>
        generate-exhaustive-vegetable-taxonomy --base-taxonomy <path> --vegetable-inventory <path> --taxonomy-version <version> --reviewed-at <date> --reviewed-by <name> --output <path>
        finalize-approved-taxonomy --candidate-taxonomy <path> --illustration-audit <path> --taxonomy-version <version> --approved-at <date> --approved-by <name> --output <path>
        audit-history --taxonomy <path> --retail-mappings <path> --policy <path> --wholesale-dir <path> --retail-dir <path> --generated-at <ISO instant> --output <path>
        generate-model-artifact --audit <path> --policy <path> --artifact-version <version> --output <path>
        audit-taxonomy --taxonomy <path> --snapshot <path> --generated-at <ISO instant> --output-dir <path>
        """.trimIndent(),
    )
}
