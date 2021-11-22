package org.hildan.bob.utils

import java.awt.Desktop
import java.net.URI

object OS {
    val isWindows = System.getProperty("os.name")?.contains("windows", ignoreCase = true) ?: false
}

fun browseIfSupported(url: String) {
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(URI(url))
    }
}

suspend fun setWindowsEnv(name: String, value: String) {
    // we escape the double quotes in the command string in order to pass the quotes to powershell
    val result = exec("powershell.exe", """[Environment]::SetEnvironmentVariable(\"$name\", \"$value\", \"User\")""")
    if (result.code != 0) {
        error("Failed to set environment variable $name. Error output:\n${result.errors}")
    }
}
