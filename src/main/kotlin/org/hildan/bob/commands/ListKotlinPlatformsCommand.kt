package org.hildan.bob.commands

import com.github.ajalt.clikt.command.*
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.hildan.bob.http.http
import kotlin.io.path.readText

private val lenientJson = Json { ignoreUnknownKeys = true }

class ListKotlinPlatformsCommand : SuspendingCliktCommand(name = "list-kotlin-platforms") {

    private val moduleFile by option(
        "-f", "--module-file",
        help = "The .module file to inspect (required if -m is not specified)",
    ).path(
        mustExist = true,
        mustBeReadable = true,
        canBeDir = false,
    )

    private val moduleId by option(
        "-m", "--module",
        help = "The group:artifactId coordinates for the module to inspect (required if -f is not specified)",
    ).convert { value ->
        val (groupId, artifactId) = value.split(":")
        ModuleId(groupId, artifactId)
    }

    private val version by option(
        "--module-version",
        help = "The version of the module to check, if -m is specified. By default, the latest version will be checked"
    )

    override fun help(context: Context): String = "Lists the Kotlin platforms supported by a given maven module"

    override suspend fun run() {
        fetchModule()
            .variants
            .map { it.targetInformation }
            .distinct()
            .sorted()
            .forEach {
                echo(it)
            }
    }

    private suspend fun fetchModule(): Module = lenientJson.decodeFromString(fetchModuleJsonContent())

    private suspend fun fetchModuleJsonContent(): String = moduleFile?.readText() ?: fetchModuleInfoFromMavenCentral()

    private suspend fun fetchModuleInfoFromMavenCentral(): String {
        val mid = moduleId ?: throw UsageError("Either -f or -m must be provided")
        return version?.let { mid.fetchModuleInfo(it) } ?: mid.fetchLatestModuleInfo()
    }
}

private val latestVersionRegex = Regex("<latest>([^<]+)</latest>")

data class ModuleId(
    val groupId: String,
    val artifactId: String,
) {
    private val groupPath = groupId.replace('.', '/')
    private val baseUrl = "https://repo1.maven.org/maven2/$groupPath/$artifactId"

    suspend fun fetchLatestModuleInfo(): String = fetchModuleInfo(fetchLatestVersion())

    suspend fun fetchModuleInfo(version: String): String = http.get("$baseUrl/$version/$artifactId-$version.module").bodyAsText()

    private suspend fun fetchLatestVersion(): String {
        val xml = http.get("$baseUrl/maven-metadata.xml").bodyAsText()
        val match = latestVersionRegex.find(xml) ?: error("Latest version not found in XML:\n$xml")
        return match.groupValues[1]
    }
}

@Serializable
data class Module(
    val variants: List<ModuleVariant>,
)

@Serializable
data class ModuleVariant(
    val name: String,
    val attributes: Map<String, JsonPrimitive>,
) {
    private val platformType: String
        get() = attributes.getValue("org.jetbrains.kotlin.platform.type").content

    private val nativeTarget: String
        get() {
            require(platformType == "native") {
                "nativeTarget is only present when platform type is 'native'"
            }
            return attributes.getValue("org.jetbrains.kotlin.native.target").content
        }

    private val jsCompiler: String
        get() {
            require(platformType == "js") {
                "nativeTarget is only present when platform type is 'js'"
            }
            return attributes.getValue("org.jetbrains.kotlin.js.compiler").content
        }

    val targetInformation: String
        get() = when (platformType) {
            "js" -> "js $jsCompiler"
            "native" -> nativeTarget
            else -> platformType
        }
}
