package org.hildan.github.secrets.wizard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.net.URI

fun browseIfSupported(url: String) {
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(URI(url))
    }
}

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun setWindowsEnv(name: String, value: String) = withContext(Dispatchers.IO) {
    // we escape the double quotes in the command string in order to pass the quotes to powershell
    val result = exec("powershell.exe", """[Environment]::SetEnvironmentVariable(\"$name\", \"$value\", \"User\")""")
    if (result.code != 0) {
        error("Failed to set environment variable $name. Error output:\n${result.errors}")
    }
}

@Suppress("BlockingMethodInNonBlockingContext")
private suspend fun exec(vararg command: String): ProcessResult = coroutineScope {
    val p = withContext(Dispatchers.IO) { ProcessBuilder(*command).start() }
    val deferredOutput = async { p.inputStream.bufferedReader().readText() }
    val deferredErrors = async { p.errorStream.bufferedReader().readText() }
    val resultCode = withContext(Dispatchers.IO) { p.waitFor() }
    val output = deferredOutput.await()
    val errors = deferredErrors.await()
    ProcessResult(resultCode, output, errors)
}

private data class ProcessResult(
    val code: Int,
    val output: String,
    val errors: String,
)
