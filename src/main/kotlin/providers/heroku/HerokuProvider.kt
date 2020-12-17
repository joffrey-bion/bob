package org.hildan.github.secrets.wizard.providers.heroku

import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import org.hildan.github.secrets.wizard.providers.Secret
import org.hildan.github.secrets.wizard.providers.SecretOptionGroup
import org.hildan.github.secrets.wizard.providers.SecretProvider

open class HerokuSecretsDefinition : SecretOptionGroup(
    providerName = "Heroku",
    switchName = "--heroku",
) {
    override val secretNames: List<String>
        get() = listOf(keySecretName)

    protected val keySecretName by option(
        "--heroku-key-secret-name",
        help = "The name of the secret variable holding the Heroku API key",
    ).default("HEROKU_API_KEY")
}

class HerokuProvider : HerokuSecretsDefinition(), SecretProvider {

    private val email by option("--heroku-email", help = "Your Heroku email").prompt(text = "Your Heroku email")

    private val password by option("--heroku-password", help = "Your Heroku password").prompt(
        text = "Your Heroku password",
        hideInput = true,
    )

    override suspend fun fetchSecrets(): List<Secret> {
        val apiKey = HerokuApi.fetchApiKey(email, password)
        val secret = Secret(keySecretName, apiKey)
        return listOf(secret)
    }
}
