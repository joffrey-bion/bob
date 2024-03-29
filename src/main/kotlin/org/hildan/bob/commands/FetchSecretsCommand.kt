package org.hildan.bob.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import kotlinx.coroutines.runBlocking
import org.hildan.bob.secrets.*
import org.hildan.bob.utils.OS
import org.hildan.bob.utils.setWindowsEnv

class FetchSecretsCommand : CliktCommand(
    name = "fetch-secrets",
    help = "Fetches secrets from various providers (Bintray, OSS Sonatype, Heroku...)",
) {
    private val defaultUser by option(
        "-u",
        "--default-user",
        help = "The default username to use when the username is not provided for one provider"
    )

    private val heroku by secretProviderGroupSwitch(HerokuProvider())

    private val sonatype by secretProviderGroupSwitch(SonatypeProvider(terminal) { defaultUser })

    private val providers: List<SecretProvider>
        get() = listOfNotNull(heroku, sonatype)

    private val destination by option(
        "-o",
        "--output",
        help = "Defines where to put the fetched secrets.",
    ).enum<Store>().default(Store.STDOUT)

    override fun run() = runBlocking {
        val secrets = providers.flatMap {
            print("Fetching secrets from ${it.providerName}...")
            it.fetchSecrets().also { println("Done.") }
        }
        when(destination) {
            Store.STDOUT -> println(secrets.joinToString("\n") { "${it.name}=${it.value}" })
            Store.ENV -> setWindowsEnv(secrets)
        }
    }

    private suspend fun setWindowsEnv(secrets: List<Secret>) {
        if (!OS.isWindows) {
            throw PrintMessage("ENV storage is only supported on Windows", printError = true)
        }
        secrets.forEach { setWindowsEnv(it.name, it.value) }
    }
}

enum class Store {
    STDOUT,
    ENV,
}
