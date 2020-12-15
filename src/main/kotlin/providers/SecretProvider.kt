package org.hildan.github.secrets.wizard.providers

interface SecretProvider<T> {

    val name: String

    fun options(defaultUserLazy: () -> String?): T

    suspend fun fetchSecrets(options: T): List<Secret>
}

data class Secret(
    val name: String,
    val value: String,
)