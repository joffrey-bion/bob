package org.hildan.bob

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.CliktHelpFormatter
import org.hildan.bob.commands.FetchSecretsCommand
import org.hildan.bob.commands.SetGitHubSecretsCommand
import org.hildan.bob.commands.UpgradeGradleWrapperCommand

fun main(args: Array<String>) = Cli().subcommands(
    FetchSecretsCommand(),
    SetGitHubSecretsCommand(),
    UpgradeGradleWrapperCommand(),
).main(args)

class Cli : NoOpCliktCommand(name = "bob", help = "A helper to manage and maintain projects") {
    init {
        context {
            helpFormatter = CliktHelpFormatter(showDefaultValues = true)
        }
    }
}
