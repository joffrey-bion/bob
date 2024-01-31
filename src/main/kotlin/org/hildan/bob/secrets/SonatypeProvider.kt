package org.hildan.bob.secrets

import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.mordant.terminal.*
import org.hildan.bob.services.sonatype.Sonatype

open class SonatypeSecretsDefinition : SecretOptionGroup(
    providerName = "OSS Sonatype",
    switchName = "--sonatype",
) {
    override val secretNames: List<String>
        get() = listOf(userTokenSecretName, keySecretName)

    protected val userTokenSecretName by option(
        "--ossrh-user-secret-name",
        help = "The name of the secret variable holding the OSS Sonatype user token",
    ).default("OSSRH_USER_TOKEN")

    protected val keySecretName by option(
        "--ossrh-key-secret-name",
        help = "The name of the secret variable holding the OSS Sonatype API key",
    ).default("OSSRH_KEY")
}

class SonatypeProvider(
    val terminal: Terminal,
    val defaultUserLazy: () -> String?,
) : SonatypeSecretsDefinition(), SecretProvider {

    private val user by option(
        "--ossrh-login",
        help = "Your OSS Sonatype login (defaults to the GitHub username)",
    ).defaultLazy {
        val defaultUser = defaultUserLazy()
        terminal.prompt("Your OSS Sonatype login", default = defaultUser)
            ?: defaultUser
            ?: error("OSS Sonatype user required")
    }

    private val password by option(
        "--ossrh-password",
        help = "Your OSS Sonatype password",
    ).prompt(
        text = "Your OSS Sonatype password",
        hideInput = true,
    )

    override suspend fun fetchSecrets(): List<Secret> {
        val sonatypeKeys = Sonatype.fetchKeys(user, password)
        val userSecret = Secret(userTokenSecretName, sonatypeKeys.userToken)
        val keySecret = Secret(keySecretName, sonatypeKeys.apiKey)
        return listOf(userSecret, keySecret)
    }
}
