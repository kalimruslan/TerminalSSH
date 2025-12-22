package com.ruslan.terminalssh.data.ssh

import com.ruslan.terminalssh.core.common.di.IoDispatcher
import com.ruslan.terminalssh.core.common.result.Result
import com.ruslan.terminalssh.domain.model.ConnectionConfig
import com.ruslan.terminalssh.domain.model.OutputType
import com.ruslan.terminalssh.domain.model.TerminalOutput
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoSshClient @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private var isConnectedState = false
    private var currentUser = "demo"
    private var currentHost = "demo-server"
    private var currentDirectory = "/home/demo"

    private val _output = MutableSharedFlow<TerminalOutput>(
        replay = 100,
        extraBufferCapacity = 256
    )
    val output: Flow<TerminalOutput> = _output

    private val fileSystem = mapOf(
        "/home/demo" to listOf("documents", "downloads", "projects", ".bashrc", ".profile"),
        "/home/demo/documents" to listOf("readme.txt", "notes.md", "report.pdf"),
        "/home/demo/downloads" to listOf("file1.zip", "image.png"),
        "/home/demo/projects" to listOf("app", "website", "scripts"),
        "/" to listOf("bin", "etc", "home", "usr", "var", "tmp"),
        "/home" to listOf("demo", "guest")
    )

    suspend fun connect(config: ConnectionConfig): Result<Unit> = withContext(ioDispatcher) {
        delay(800)

        currentUser = config.username.ifBlank { "demo" }
        currentHost = "demo-server"
        currentDirectory = "/home/$currentUser"
        isConnectedState = true

        _output.emit(
            TerminalOutput(
                text = "Welcome to Demo SSH Server\n",
                type = OutputType.OUTPUT
            )
        )
        _output.emit(
            TerminalOutput(
                text = "Connected as $currentUser@$currentHost\n",
                type = OutputType.OUTPUT
            )
        )
        _output.emit(
            TerminalOutput(
                text = "Type 'help' for available commands\n\n",
                type = OutputType.OUTPUT
            )
        )

        Result.Success(Unit)
    }

    suspend fun executeCommand(command: String): Result<Unit> = withContext(ioDispatcher) {
        if (!isConnectedState) {
            return@withContext Result.Error(IllegalStateException("Not connected"))
        }

        delay(100)

        val output = processCommand(command.trim())

        if (output.isNotEmpty()) {
            _output.emit(
                TerminalOutput(
                    text = output + "\n",
                    type = if (output.startsWith("bash:")) OutputType.ERROR else OutputType.OUTPUT
                )
            )
        }

        Result.Success(Unit)
    }

    private fun processCommand(command: String): String {
        val parts = command.split(" ").filter { it.isNotBlank() }
        if (parts.isEmpty()) return ""

        val cmd = parts[0].lowercase()
        val args = parts.drop(1)

        return when (cmd) {
            "help" -> getHelpText()
            "ls" -> handleLs(args)
            "pwd" -> currentDirectory
            "whoami" -> currentUser
            "hostname" -> currentHost
            "date" -> SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US).format(Date())
            "uname" -> handleUname(args)
            "echo" -> args.joinToString(" ").replace("\"", "").replace("'", "")
            "cd" -> handleCd(args)
            "cat" -> handleCat(args)
            "clear" -> ""
            "id" -> "uid=1000($currentUser) gid=1000($currentUser) groups=1000($currentUser),27(sudo)"
            "uptime" -> " ${SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())} up 42 days, 3:14, 1 user, load average: 0.08, 0.03, 0.01"
            "df" -> getDfOutput()
            "free" -> getFreeOutput()
            "ps" -> getPsOutput()
            "env" -> getEnvOutput()
            "exit", "logout" -> "logout\nConnection closed."
            else -> "bash: $cmd: command not found"
        }
    }

    private fun getHelpText(): String = """
        |Available commands in demo mode:
        |  help     - Show this help message
        |  ls       - List directory contents
        |  pwd      - Print working directory
        |  cd       - Change directory
        |  whoami   - Print current user
        |  hostname - Print hostname
        |  date     - Print current date and time
        |  uname    - Print system information
        |  echo     - Print arguments
        |  cat      - Print file contents (demo)
        |  clear    - Clear terminal screen
        |  id       - Print user identity
        |  uptime   - Show system uptime
        |  df       - Show disk usage
        |  free     - Show memory usage
        |  ps       - Show running processes
        |  env      - Show environment variables
        |  exit     - Close connection
    """.trimMargin()

    private fun handleLs(args: List<String>): String {
        val showAll = args.contains("-a") || args.contains("-la") || args.contains("-al")
        val showLong = args.contains("-l") || args.contains("-la") || args.contains("-al")

        val targetDir = args.lastOrNull { !it.startsWith("-") } ?: currentDirectory
        val resolvedDir = resolvePath(targetDir)

        val files = fileSystem[resolvedDir]
            ?: return "ls: cannot access '$targetDir': No such file or directory"

        val allFiles = if (showAll) listOf(".", "..") + files else files.filter { !it.startsWith(".") }

        return if (showLong) {
            allFiles.joinToString("\n") { file ->
                val isDir = !file.contains(".") || file == "." || file == ".."
                val perms = if (isDir) "drwxr-xr-x" else "-rw-r--r--"
                val size = if (isDir) "4096" else "${(100..9999).random()}"
                "$perms 1 $currentUser $currentUser ${size.padStart(5)} Jan 15 10:30 $file"
            }
        } else {
            allFiles.joinToString("  ")
        }
    }

    private fun handleCd(args: List<String>): String {
        if (args.isEmpty()) {
            currentDirectory = "/home/$currentUser"
            return ""
        }

        val target = args[0]
        val newPath = resolvePath(target)

        return if (fileSystem.containsKey(newPath) || newPath == "/home/$currentUser") {
            currentDirectory = newPath
            ""
        } else {
            "bash: cd: $target: No such file or directory"
        }
    }

    private fun handleCat(args: List<String>): String {
        if (args.isEmpty()) return ""

        return when (args[0]) {
            "readme.txt", "./readme.txt", "documents/readme.txt" ->
                "Welcome to TerminalSSH Demo Mode!\nThis is a simulated SSH environment for testing purposes."

            ".bashrc" ->
                "# ~/.bashrc\nexport PATH=\$PATH:/usr/local/bin\nalias ll='ls -la'"

            else -> "cat: ${args[0]}: No such file or directory"
        }
    }

    private fun handleUname(args: List<String>): String {
        return when {
            args.contains("-a") -> "Linux $currentHost 5.15.0-demo #1 SMP Demo x86_64 GNU/Linux"
            args.contains("-r") -> "5.15.0-demo"
            args.contains("-n") -> currentHost
            args.contains("-s") -> "Linux"
            args.contains("-m") -> "x86_64"
            else -> "Linux"
        }
    }

    private fun resolvePath(path: String): String {
        return when {
            path == "~" || path == "" -> "/home/$currentUser"
            path.startsWith("~/") -> "/home/$currentUser" + path.substring(1)
            path.startsWith("/") -> path
            path == ".." -> {
                val parts = currentDirectory.split("/").filter { it.isNotEmpty() }
                if (parts.size <= 1) "/" else "/" + parts.dropLast(1).joinToString("/")
            }

            path == "." -> currentDirectory
            else -> "$currentDirectory/$path".replace("//", "/")
        }
    }

    private fun getDfOutput(): String = """
        |Filesystem     1K-blocks    Used Available Use% Mounted on
        |/dev/sda1       51475068 8234512  40602556  17% /
        |tmpfs            4028256       0   4028256   0% /dev/shm
        |/dev/sda2       10190136 1024576   8625848  11% /home
    """.trimMargin()

    private fun getFreeOutput(): String = """
        |              total        used        free      shared  buff/cache   available
        |Mem:        8056512     2048256     4028256      128512     1980000     5632000
        |Swap:       2097148           0     2097148
    """.trimMargin()

    private fun getPsOutput(): String = """
        |  PID TTY          TIME CMD
        |    1 pts/0    00:00:00 bash
        |  123 pts/0    00:00:00 ps
    """.trimMargin()

    private fun getEnvOutput(): String = """
        |USER=$currentUser
        |HOME=/home/$currentUser
        |SHELL=/bin/bash
        |PATH=/usr/local/bin:/usr/bin:/bin
        |TERM=xterm-256color
        |HOSTNAME=$currentHost
        |PWD=$currentDirectory
    """.trimMargin()

    suspend fun disconnect() = withContext(ioDispatcher) {
        isConnectedState = false
        currentDirectory = "/home/demo"
        _output.resetReplayCache()
    }

    fun isConnected(): Boolean = isConnectedState
}
