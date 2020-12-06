package org.hildan.github.secrets.wizard

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.CliktHelpFormatter
import org.hildan.github.secrets.wizard.commands.GitHubSecretCommand
import org.hildan.github.secrets.wizard.commands.SetEnvironmentKeysCommand

fun main(args: Array<String>) = Cli().subcommands(GitHubSecretCommand(), SetEnvironmentKeysCommand()).main(args)

class Cli : NoOpCliktCommand(name = "dev", help = "Local development utilities") {
    init {
        context {
            helpFormatter = CliktHelpFormatter(showDefaultValues = true)
        }
    }
}
