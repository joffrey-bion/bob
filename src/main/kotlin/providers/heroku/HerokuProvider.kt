package org.hildan.github.secrets.wizard.providers.heroku

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import org.hildan.github.secrets.wizard.providers.Secret
import org.hildan.github.secrets.wizard.providers.SecretProvider

class HerokuOptions : OptionGroup(name = "Options for Heroku secrets") {

    val email by option("--heroku-email", help = "Your Heroku email").prompt(text = "Your Heroku email")

    val password by option("--heroku-password", help = "Your Heroku password").prompt(
        text = "Your Heroku password",
        hideInput = true,
    )

    val keySecretName by option(
        "--heroku-key-secret-name",
        help = "The name of the secret variable holding the Heroku API key",
    ).default("HEROKU_API_KEY")
}

object Heroku : SecretProvider<HerokuOptions> {

    override val name: String = "Heroku"

    override fun options(defaultUserLazy: () -> String?) = HerokuOptions()

    override suspend fun fetchSecrets(options: HerokuOptions): List<Secret> {
        val apiKey = HerokuApi.fetchApiKey(options.email, options.password)
        val secret = Secret(options.keySecretName, apiKey)
        return listOf(secret)
    }
}
