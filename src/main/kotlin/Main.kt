package org.hildan.github.secrets.wizard

import kotlinx.coroutines.*
import org.hildan.github.secrets.wizard.providers.Bintray
import org.hildan.github.secrets.wizard.providers.OssSonatype

fun main() {
    runBlocking {
        val githubUser = prompt("GitHub user: ")
        val githubRepo = prompt("GitHub repository to setup secrets for: ")

        val bintrayCreds = promptForCredentials("Bintray", githubUser)
        val ossCreds = promptForCredentials("OSS Sonatype", githubUser)

        val bintrayKey = Bintray.fetchApiKey(bintrayCreds.login, bintrayCreds.password)
        val sonatypeKeys = OssSonatype.fetchKeys(ossCreds.login, ossCreds.password)

        val gitHub = GitHub.login()
        val repo = GitHubRepo(githubUser, githubRepo)
        gitHub.setSecret("BINTRAY_USER", bintrayCreds.login, repo)
        gitHub.setSecret("BINTRAY_KEY", bintrayKey, repo)
        gitHub.setSecret("OSSRH_USER_TOKEN", sonatypeKeys.userToken, repo)
        gitHub.setSecret("OSSRH_KEY", sonatypeKeys.apiKey, repo)
    }
}

private fun prompt(promptText: String): String {
    print(promptText)
    return readLine()!!
}

private fun promptForCredentials(orgName: String, defaultUser: String): Credentials {
    println("Enter your credentials for $orgName:")
    print("  Login ($defaultUser): ")
    val login = readLine()!!.ifEmpty { defaultUser }
    print("  Password: ")
    val password = System.console()?.readPassword()?.toString() ?: readLine()!!
    return Credentials(login, password)
}

private data class Credentials(val login: String, val password: String)
