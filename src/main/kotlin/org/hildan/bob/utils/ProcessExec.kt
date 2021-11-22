package org.hildan.bob.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

suspend fun exec(vararg command: String): ProcessResult = withContext(Dispatchers.IO) {
    val p = ProcessBuilder(*command).start()
    val deferredOutput = async { p.inputStream.bufferedReader().readText() }
    val deferredErrors = async { p.errorStream.bufferedReader().readText() }
    val resultCode = p.waitFor()
    val output = deferredOutput.await()
    val errors = deferredErrors.await()
    ProcessResult(resultCode, output, errors)
}

data class ProcessResult(
    val code: Int,
    val output: String,
    val errors: String,
)
