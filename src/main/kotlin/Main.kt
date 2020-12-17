package org.hildan.github.secrets.wizard

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.CliktHelpFormatter
import org.hildan.github.secrets.wizard.commands.FetchSecretsCommand
import org.hildan.github.secrets.wizard.commands.SetGitHubSecretsCommand

fun main(args: Array<String>) = Cli().subcommands(
    FetchSecretsCommand(),
    SetGitHubSecretsCommand(),
).main(args)

class Cli : NoOpCliktCommand(name = "secrets-wizard", help = "A helper to fetch and set secrets") {
    init {
        context {
            helpFormatter = CliktHelpFormatter(showDefaultValues = true)
        }
    }
}
