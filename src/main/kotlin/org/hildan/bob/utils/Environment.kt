package org.hildan.bob.utils

object OS {
    val isWindows = System.getProperty("os.name")?.contains("windows", ignoreCase = true) ?: false
}
