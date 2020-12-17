package org.hildan.github.secrets.wizard.providers

import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.groupSwitch
import com.github.ajalt.clikt.parameters.options.option

interface SecretGroupDefinition {

    val providerName: String

    val switchName: String

    val secretNames: List<String>
}

interface SecretProvider : SecretGroupDefinition {

    suspend fun fetchSecrets(): List<Secret>
}

data class Secret(
    val name: String,
    val value: String,
)

abstract class SecretOptionGroup(
    override val providerName: String,
    override val switchName: String,
) : OptionGroup(
    name = "$providerName options",
    help = "Options for $providerName secrets (enable with $switchName)",
), SecretGroupDefinition

fun <T> ParameterHolder.secretsDefinitionGroupSwitch(provider: T) where T : SecretGroupDefinition, T : OptionGroup =
    option(help = "Enables setting secrets for ${provider.providerName}").groupSwitch(provider.switchName to provider)

fun <T> ParameterHolder.secretProviderGroupSwitch(provider: T) where T : SecretProvider, T : OptionGroup =
    option(help = "Enables fetching from ${provider.providerName}").groupSwitch(provider.switchName to provider)
