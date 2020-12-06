package org.hildan.github.secrets.wizard.commands

import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt

class BintraySecretOptions(
    val defaultUserLazy: () -> String
) : OptionGroup(
    name = "Options for Bintray secrets",
    help = "Options to setup the Bintray API key as a secret (enable with --bintray)",
) {
    val user by option("--bintray-login", help = "Your Bintray login (defaults to the GitHub username)").defaultLazy {
        val defaultUser = defaultUserLazy()
        TermUi.prompt("Your Bintray login", default = defaultUser) ?: defaultUser
    }

    val password by option("--bintray-password", help = "Your Bintray password").prompt(
        text = "Your Bintray password",
        hideInput = true,
    )

    val userSecretName by option(
        "--bintray-user-secret-name",
        help = "The name of the secret variable holding the Bintray username",
    ).default("BINTRAY_USER")

    val keySecretName by option(
        "--bintray-key-secret-name",
        help = "The name of the secret variable holding the Bintray API key",
    ).default("BINTRAY_KEY")
}

class SonatypeSecretOptions(
    val defaultUserLazy: () -> String
) : OptionGroup(
    name = "Options for OSS Sonatype secrets",
    help = "Options to setup the OSS Sonatype user token and key as secrets (enable with --sonatype)",
) {
    val user by option("--ossrh-login", help = "Your OSS Sonatype login (defaults to the GitHub username)").defaultLazy {
        val defaultUser = defaultUserLazy()
        TermUi.prompt("Your OSS Sonatype login", default = defaultUser) ?: defaultUser
    }

    val password by option("--ossrh-password", help = "Your OSS Sonatype password").prompt(
        text = "Your OSS Sonatype password",
        hideInput = true,
    )

    val userTokenSecretName by option(
        "--ossrh-user-secret-name",
        help = "The name of the secret variable holding the OSS Sonatype user token",
    ).default("OSSRH_USER_TOKEN")

    val keySecretName by option(
        "--ossrh-key-secret-name",
        help = "The name of the secret variable holding the OSS Sonatype API key",
    ).default("OSSRH_KEY")
}
