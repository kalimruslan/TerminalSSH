package com.ruslan.terminalssh.data.ssh

import android.content.Context
import com.ruslan.terminalssh.core.common.di.IoDispatcher
import com.ruslan.terminalssh.core.common.result.Result
import com.ruslan.terminalssh.domain.model.ConnectionConfig
import com.ruslan.terminalssh.domain.model.OutputType
import com.ruslan.terminalssh.domain.model.TerminalOutput
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.sshd.client.SshClient as MinaSshClient
import org.apache.sshd.client.channel.ChannelShell
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.common.util.io.PathUtils
import java.io.OutputStream
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SshClient @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private var client: MinaSshClient? = null
    private var session: ClientSession? = null
    private var channel: ChannelShell? = null
    private var shellOutputStream: OutputStream? = null
    private var readerJob: Job? = null

    private val _output = MutableSharedFlow<TerminalOutput>(
        replay = 100,
        extraBufferCapacity = 256
    )
    val output: Flow<TerminalOutput> = _output

    // Regex to strip ANSI escape sequences (более полный паттерн)
    private val ansiEscapeRegex = Regex(
        "\u001B\\[[0-9;?]*[a-zA-Z]|" +      // CSI sequences: \e[...X
        "\u001B\\][^\u0007]*\u0007|" +       // OSC sequences: \e]...\a
        "\u001B[()][AB012]|" +               // Character set: \e(B etc
        "\u001B[=>]|" +                      // Keypad modes
        "\u001B[78]|" +                      // Save/restore cursor
        "\u001B\\[\\?[0-9;]*[hl]|" +         // DEC Private modes
        "\r"                                  // Carriage return
    )

    init {
        setupAndroidPaths()
    }

    private fun stripAnsiCodes(text: String): String {
        return ansiEscapeRegex.replace(text, "")
    }

    private fun setupAndroidPaths() {
        val homeDir = context.filesDir.absolutePath
        PathUtils.setUserHomeFolderResolver { Paths.get(homeDir) }
    }

    suspend fun connect(config: ConnectionConfig): Result<Unit> = withContext(ioDispatcher) {
        try {
            disconnect()

            client = MinaSshClient.setUpDefaultClient().apply {
                start()
            }

            session = client?.connect(config.username, config.host, config.port)
                ?.verify(30, TimeUnit.SECONDS)
                ?.session
                ?.apply {
                    addPasswordIdentity(config.password)
                    auth().verify(30, TimeUnit.SECONDS)
                }

            channel = session?.createShellChannel()?.apply {
                setPtyType("dumb")
                setPtyColumns(200)
                setPtyLines(50)
                setEnv("TERM", "dumb")
                open().verify(10, TimeUnit.SECONDS)
            }

            shellOutputStream = channel?.invertedIn

            startOutputReader()

            Result.Success(Unit)
        } catch (e: Exception) {
            disconnect()
            Result.Error(e)
        }
    }

    private fun startOutputReader() {
        val ch = channel ?: return

        readerJob = CoroutineScope(ioDispatcher).launch {
            try {
                val buffer = ByteArray(4096)
                val inputStream = ch.invertedOut

                while (isActive && ch.isOpen) {
                    val available = inputStream.available()
                    if (available > 0) {
                        val read = inputStream.read(buffer, 0, minOf(available, buffer.size))
                        if (read > 0) {
                            val rawText = String(buffer, 0, read, Charsets.UTF_8)
                            val cleanText = stripAnsiCodes(rawText)
                            if (cleanText.isNotEmpty()) {
                                _output.emit(TerminalOutput(text = cleanText, type = OutputType.OUTPUT))
                            }
                        }
                    } else {
                        kotlinx.coroutines.delay(50)
                    }
                }
            } catch (e: Exception) {
                if (channel?.isOpen == true) {
                    _output.emit(TerminalOutput(text = "Error: ${e.message}", type = OutputType.ERROR))
                }
            }
        }
    }

    suspend fun executeCommand(command: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val outputStream = shellOutputStream
                ?: return@withContext Result.Error(IllegalStateException("Not connected"))

            outputStream.write("$command\n".toByteArray(Charsets.UTF_8))
            outputStream.flush()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun disconnect() = withContext(ioDispatcher) {
        try {
            readerJob?.cancel()
            readerJob = null

            shellOutputStream = null

            channel?.close(false)
            channel = null

            session?.close(false)
            session = null

            client?.stop()
            client = null

            // Очищаем replay буфер, чтобы при следующем подключении
            // не показывались старые сообщения
            _output.resetReplayCache()
        } catch (e: Exception) {
            // Ignore errors during disconnect
        }
    }

    fun isConnected(): Boolean = session?.isOpen == true && channel?.isOpen == true
}
