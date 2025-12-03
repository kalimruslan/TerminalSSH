# Архитектура проекта TerminalSSH

## Содержание

1. [Обзор архитектуры](#1-обзор-архитектуры)
2. [Модульная структура](#2-модульная-структура)
3. [Слои приложения](#3-слои-приложения)
   - [Domain Layer](#31-domain-layer)
   - [Data Layer](#32-data-layer)
   - [Presentation Layer](#33-presentation-layer)
4. [Dependency Injection](#4-dependency-injection)
5. [Навигация](#5-навигация)
6. [Управление состоянием (MVI)](#6-управление-состоянием-mvi)
7. [Используемые библиотеки](#7-используемые-библиотеки)
8. [Потоки данных](#8-потоки-данных)

---

## 1. Обзор архитектуры

Проект построен на основе **Clean Architecture** в сочетании с паттерном **MVI (Model-View-Intent)** для управления состоянием UI.

```
┌─────────────────────────────────────────────────────────────────────┐
│                      PRESENTATION LAYER                             │
│  ┌──────────────┐   ┌──────────────┐   ┌────────────────────────┐  │
│  │   Screens    │ → │  ViewModels  │ → │   State / Intent /     │  │
│  │  (Compose)   │ ← │    (MVI)     │ ← │       Effect           │  │
│  └──────────────┘   └──────────────┘   └────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        DOMAIN LAYER                                 │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                       Use Cases                               │  │
│  │  ConnectSsh • DisconnectSsh • ExecuteCommand • SaveConnection │  │
│  └──────────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                  Repository Interfaces                        │  │
│  │       SshRepository • ConnectionRepository • FavoriteRepo     │  │
│  └──────────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                       Domain Models                           │  │
│  │  ConnectionConfig • ConnectionState • TerminalOutput • etc    │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         DATA LAYER                                  │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────────┐    │
│  │  Repository    │  │   SSH Client   │  │    Room Database   │    │
│  │  Implementations│  │  (Apache MINA) │  │    (Connections)   │    │
│  └────────────────┘  └────────────────┘  └────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
```

### Принципы архитектуры

1. **Dependency Rule** — зависимости направлены внутрь (Presentation → Domain ← Data)
2. **Separation of Concerns** — каждый слой имеет чётко определённую ответственность
3. **Testability** — бизнес-логика изолирована и легко тестируется
4. **Single Source of Truth** — данные текут в одном направлении

---

## 2. Модульная структура

```
TerminalSSH/
├── app/                          # Главный модуль приложения
│   └── src/main/java/.../
│       ├── MainActivity.kt       # Единственная Activity
│       └── TerminalSshApplication.kt
│
├── core/                         # Общие модули
│   ├── common/                   # Утилиты, DI Dispatchers, Result wrapper
│   │   └── src/main/java/.../
│   │       ├── di/
│   │       │   └── DispatchersModule.kt
│   │       └── result/
│   │           └── Result.kt
│   │
│   └── theme/                    # Material Design 3 тема
│       └── src/main/java/.../
│           ├── Color.kt
│           ├── Theme.kt
│           ├── Type.kt
│           └── TerminalColors.kt
│
├── domain/                       # Бизнес-логика (чистый Kotlin)
│   └── src/main/java/.../
│       ├── model/                # Domain модели
│       ├── repository/           # Интерфейсы репозиториев
│       └── usecase/              # Use Cases
│
├── data/                         # Реализация данных
│   └── src/main/java/.../
│       ├── database/             # Room (Entity, DAO, Database)
│       ├── di/                   # Hilt модули
│       ├── repository/           # Реализации репозиториев
│       └── ssh/                  # SSH клиент
│
└── feature/                      # Feature модули
    └── terminal/                 # Модуль терминала
        └── src/main/java/.../
            ├── connect/          # Экран подключения
            ├── terminal/         # Экран терминала
            └── navigation/       # Навигация
```

### Зависимости между модулями

```
         ┌─────────────────────────────┐
         │            app              │
         └──────────────┬──────────────┘
                        │
         ┌──────────────┼──────────────┐
         │              │              │
         ▼              ▼              ▼
┌─────────────┐  ┌───────────┐  ┌───────────┐
│feature:terminal│  │   data    │  │core:theme │
└───────┬─────┘  └─────┬─────┘  └───────────┘
        │              │
        │              │
        ▼              ▼
    ┌───────────────────────┐
    │        domain         │
    └───────────┬───────────┘
                │
                ▼
         ┌────────────┐
         │core:common │
         └────────────┘
```

---

## 3. Слои приложения

### 3.1 Domain Layer

Domain Layer — это ядро приложения, содержащее бизнес-логику. **Не зависит от Android SDK**.

#### 3.1.1 Domain Models

Модели данных, описывающие основные сущности:

```kotlin
// domain/model/ConnectionConfig.kt
data class ConnectionConfig(
    val host: String,
    val port: Int = 22,
    val username: String,
    val password: String
)

// domain/model/ConnectionState.kt
sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

// domain/model/TerminalOutput.kt
data class TerminalOutput(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val type: OutputType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class OutputType {
    COMMAND,    // Введённая пользователем команда
    OUTPUT,     // Вывод от сервера
    ERROR       // Сообщение об ошибке
}

// domain/model/SavedConnection.kt
data class SavedConnection(
    val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val lastUsedAt: Long? = null
)

// domain/model/FavoriteCommand.kt
data class FavoriteCommand(
    val id: Long = 0,
    val connectionId: Long,
    val command: String,
    val description: String? = null
)
```

#### 3.1.2 Repository Interfaces

Интерфейсы репозиториев определяют контракт для работы с данными:

```kotlin
// domain/repository/SshRepository.kt
interface SshRepository {
    val connectionState: StateFlow<ConnectionState>
    val terminalOutput: Flow<TerminalOutput>

    suspend fun connect(config: ConnectionConfig): Result<Unit>
    suspend fun disconnect()
    suspend fun executeCommand(command: String): Result<Unit>
}

// domain/repository/ConnectionRepository.kt
interface ConnectionRepository {
    fun getAllConnections(): Flow<List<SavedConnection>>
    suspend fun getById(id: Long): SavedConnection?
    suspend fun save(connection: SavedConnection): Long
    suspend fun delete(connection: SavedConnection)
    suspend fun updateLastUsed(id: Long)
}

// domain/repository/FavoriteCommandRepository.kt
interface FavoriteCommandRepository {
    fun getCommandsForConnection(connectionId: Long): Flow<List<FavoriteCommand>>
    suspend fun addToFavorites(connectionId: Long, command: String, description: String? = null)
    suspend fun removeFromFavorites(connectionId: Long, command: String)
    suspend fun isFavorite(connectionId: Long, command: String): Boolean
}
```

#### 3.1.3 Use Cases

Use Cases инкапсулируют бизнес-операции:

```kotlin
// domain/usecase/ConnectSshUseCase.kt
class ConnectSshUseCase @Inject constructor(
    private val repository: SshRepository
) {
    suspend operator fun invoke(config: ConnectionConfig): Result<Unit> {
        return repository.connect(config)
    }
}

// domain/usecase/ExecuteCommandUseCase.kt
class ExecuteCommandUseCase @Inject constructor(
    private val repository: SshRepository
) {
    suspend operator fun invoke(command: String): Result<Unit> {
        return repository.executeCommand(command)
    }
}

// domain/usecase/GetSavedConnectionsUseCase.kt
class GetSavedConnectionsUseCase @Inject constructor(
    private val repository: ConnectionRepository
) {
    operator fun invoke(): Flow<List<SavedConnection>> {
        return repository.getAllConnections()
    }
}

// domain/usecase/AddToFavoritesUseCase.kt
class AddToFavoritesUseCase @Inject constructor(
    private val repository: FavoriteCommandRepository
) {
    suspend operator fun invoke(
        connectionId: Long,
        command: String,
        description: String? = null
    ) {
        repository.addToFavorites(connectionId, command, description)
    }
}
```

**Полный список Use Cases:**
- `ConnectSshUseCase` — подключение к SSH
- `DisconnectSshUseCase` — отключение
- `ExecuteCommandUseCase` — выполнение команды
- `GetSavedConnectionsUseCase` — список сохранённых подключений
- `SaveConnectionUseCase` — сохранение подключения
- `DeleteConnectionUseCase` — удаление подключения
- `GetConnectionByIdUseCase` — получение подключения по ID
- `UpdateConnectionLastUsedUseCase` — обновление времени последнего использования
- `GetFavoriteCommandsUseCase` — получение избранных команд
- `AddToFavoritesUseCase` — добавление в избранное
- `RemoveFromFavoritesUseCase` — удаление из избранного

---

### 3.2 Data Layer

Data Layer отвечает за работу с внешними источниками данных.

#### 3.2.1 SSH Client

SSH клиент использует библиотеку **Apache MINA SSHD**:

```kotlin
// data/ssh/SshClient.kt
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

    // SharedFlow для потоковой передачи вывода терминала
    private val _output = MutableSharedFlow<TerminalOutput>(
        replay = 100,           // Хранить последние 100 сообщений
        extraBufferCapacity = 256
    )
    val output: Flow<TerminalOutput> = _output

    // Regex для очистки ANSI escape-последовательностей
    private val ansiEscapeRegex = Regex(
        "\u001B\\[[0-9;?]*[a-zA-Z]|" +  // CSI sequences
        "\u001B\\][^\u0007]*\u0007|" +   // OSC sequences
        "\u001B[()][AB012]|" +           // Character set
        "\u001B[=>]|" +                  // Keypad modes
        "\u001B[78]|" +                  // Save/restore cursor
        "\u001B\\[\\?[0-9;]*[hl]|" +     // DEC Private modes
        "\r"                              // Carriage return
    )

    suspend fun connect(config: ConnectionConfig): Result<Unit> = withContext(ioDispatcher) {
        try {
            disconnect()

            // Создание и запуск SSH клиента
            client = MinaSshClient.setUpDefaultClient().apply {
                start()
            }

            // Установка соединения и аутентификация
            session = client?.connect(config.username, config.host, config.port)
                ?.verify(30, TimeUnit.SECONDS)
                ?.session
                ?.apply {
                    addPasswordIdentity(config.password)
                    auth().verify(30, TimeUnit.SECONDS)
                }

            // Создание shell канала с TTY
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
                                _output.emit(TerminalOutput(
                                    text = cleanText,
                                    type = OutputType.OUTPUT
                                ))
                            }
                        }
                    } else {
                        delay(50)  // Polling interval
                    }
                }
            } catch (e: Exception) {
                if (channel?.isOpen == true) {
                    _output.emit(TerminalOutput(
                        text = "Error: ${e.message}",
                        type = OutputType.ERROR
                    ))
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
        readerJob?.cancel()
        channel?.close(false)
        session?.close(false)
        client?.stop()
        _output.resetReplayCache()
    }
}
```

#### 3.2.2 Repository Implementations

```kotlin
// data/repository/SshRepositoryImpl.kt
@Singleton
class SshRepositoryImpl @Inject constructor(
    private val sshClient: SshClient
) : SshRepository {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    override val terminalOutput: Flow<TerminalOutput> = sshClient.output

    override suspend fun connect(config: ConnectionConfig): Result<Unit> {
        _connectionState.value = ConnectionState.Connecting

        return when (val result = sshClient.connect(config)) {
            is Result.Success -> {
                _connectionState.value = ConnectionState.Connected
                Result.Success(Unit)
            }
            is Result.Error -> {
                _connectionState.value = ConnectionState.Error(
                    result.exception.message ?: "Connection failed"
                )
                Result.Error(result.exception)
            }
            is Result.Loading -> Result.Loading
        }
    }

    override suspend fun disconnect() {
        sshClient.disconnect()
        _connectionState.value = ConnectionState.Disconnected
    }

    override suspend fun executeCommand(command: String): Result<Unit> {
        return sshClient.executeCommand(command)
    }
}
```

#### 3.2.3 Room Database

**Entity:**
```kotlin
// data/database/entity/ConnectionEntity.kt
@Entity(tableName = "connections")
data class ConnectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val createdAt: Long,
    val lastUsedAt: Long?
)

// data/database/entity/FavoriteCommandEntity.kt
@Entity(
    tableName = "favorite_commands",
    foreignKeys = [ForeignKey(
        entity = ConnectionEntity::class,
        parentColumns = ["id"],
        childColumns = ["connectionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["connectionId"])]
)
data class FavoriteCommandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val connectionId: Long,
    val command: String,
    val description: String?,
    val createdAt: Long
)
```

**DAO:**
```kotlin
// data/database/dao/ConnectionDao.kt
@Dao
interface ConnectionDao {
    @Query("SELECT * FROM connections ORDER BY lastUsedAt DESC NULLS LAST")
    fun getAllConnections(): Flow<List<ConnectionEntity>>

    @Query("SELECT * FROM connections WHERE id = :id")
    suspend fun getById(id: Long): ConnectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(connection: ConnectionEntity): Long

    @Update
    suspend fun update(connection: ConnectionEntity)

    @Delete
    suspend fun delete(connection: ConnectionEntity)

    @Query("UPDATE connections SET lastUsedAt = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: Long, timestamp: Long)
}
```

**Database:**
```kotlin
// data/database/AppDatabase.kt
@Database(
    entities = [ConnectionEntity::class, FavoriteCommandEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun connectionDao(): ConnectionDao
    abstract fun favoriteCommandDao(): FavoriteCommandDao
}
```

---

### 3.3 Presentation Layer

Presentation Layer использует **Jetpack Compose** для UI и **MVI** для управления состоянием.

#### 3.3.1 MVI Contract

Каждый экран имеет Contract с тремя компонентами:

```kotlin
// feature/terminal/terminal/TerminalContract.kt

// State — текущее состояние экрана
data class TerminalState(
    val outputs: List<TerminalOutput> = emptyList(),
    val currentCommand: String = "",
    val isConnected: Boolean = true,
    val connectionId: Long = 0,
    val favoriteCommands: List<FavoriteCommand> = emptyList(),
    val showFavoritesDropdown: Boolean = false,
    val isCurrentCommandFavorite: Boolean = false
)

// Intent — действия пользователя
sealed class TerminalIntent {
    data class UpdateCommand(val command: String) : TerminalIntent()
    data object ExecuteCommand : TerminalIntent()
    data object Disconnect : TerminalIntent()
    data object ToggleFavoritesDropdown : TerminalIntent()
    data class SelectFavoriteCommand(val command: FavoriteCommand) : TerminalIntent()
    data object ToggleCurrentCommandFavorite : TerminalIntent()
}

// Effect — одноразовые события (навигация, показ snackbar и т.д.)
sealed class TerminalEffect {
    data object NavigateBack : TerminalEffect()
    data object ScrollToBottom : TerminalEffect()
}
```

#### 3.3.2 ViewModel

```kotlin
// feature/terminal/terminal/TerminalViewModel.kt
@HiltViewModel
class TerminalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SshRepository,
    private val executeCommandUseCase: ExecuteCommandUseCase,
    private val disconnectSshUseCase: DisconnectSshUseCase,
    private val getFavoriteCommandsUseCase: GetFavoriteCommandsUseCase,
    private val addToFavoritesUseCase: AddToFavoritesUseCase,
    private val removeFromFavoritesUseCase: RemoveFromFavoritesUseCase
) : ViewModel() {

    private val connectionId: Long = savedStateHandle.get<Long>("connectionId") ?: 0L

    // State
    private val _state = MutableStateFlow(TerminalState(connectionId = connectionId))
    val state: StateFlow<TerminalState> = _state.asStateFlow()

    // Effects (one-time events)
    private val _effect = Channel<TerminalEffect>()
    val effect = _effect.receiveAsFlow()

    // Буфер для оптимизации производительности
    private val outputBuffer = mutableListOf<TerminalOutput>()
    private val bufferLock = Any()

    init {
        observeTerminalOutput()
        observeConnectionState()
        if (connectionId > 0) {
            observeFavoriteCommands()
        }
    }

    // Обработчик Intent'ов
    fun handleIntent(intent: TerminalIntent) {
        when (intent) {
            is TerminalIntent.UpdateCommand -> updateCommand(intent.command)
            is TerminalIntent.ExecuteCommand -> executeCommand()
            is TerminalIntent.Disconnect -> disconnect()
            is TerminalIntent.ToggleFavoritesDropdown -> toggleFavoritesDropdown()
            is TerminalIntent.SelectFavoriteCommand -> selectFavoriteCommand(intent.command)
            is TerminalIntent.ToggleCurrentCommandFavorite -> toggleCurrentCommandFavorite()
        }
    }

    private fun observeTerminalOutput() {
        // Collect outputs into buffer
        viewModelScope.launch {
            repository.terminalOutput.collect { output ->
                synchronized(bufferLock) {
                    outputBuffer.add(output)
                }
            }
        }

        // Flush buffer to UI every 100ms (оптимизация)
        viewModelScope.launch {
            while (true) {
                delay(100)
                val toAdd = synchronized(bufferLock) {
                    if (outputBuffer.isEmpty()) emptyList()
                    else {
                        val list = outputBuffer.toList()
                        outputBuffer.clear()
                        list
                    }
                }

                if (toAdd.isEmpty()) continue

                _state.update { currentState ->
                    // Дедупликация
                    val existingIds = currentState.outputs.map { it.id }.toSet()
                    val recentTexts = currentState.outputs.takeLast(50).map { it.text.trim() }.toSet()

                    val newItems = toAdd.filter { output ->
                        output.id !in existingIds && output.text.trim() !in recentTexts
                    }
                    if (newItems.isEmpty()) return@update currentState

                    // Ограничение размера буфера
                    val maxOutputs = 100
                    val combined = currentState.outputs + newItems
                    val trimmed = if (combined.size > maxOutputs) {
                        combined.takeLast(maxOutputs)
                    } else combined

                    currentState.copy(outputs = trimmed)
                }
                _effect.send(TerminalEffect.ScrollToBottom)
            }
        }
    }

    private fun executeCommand() {
        val command = _state.value.currentCommand.trim()
        if (command.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(currentCommand = "", showFavoritesDropdown = false) }
            executeCommandUseCase(command)
        }
    }

    private fun disconnect() {
        viewModelScope.launch {
            disconnectSshUseCase()
            _effect.send(TerminalEffect.NavigateBack)
        }
    }
}
```

#### 3.3.3 Compose Screen

```kotlin
// feature/terminal/terminal/TerminalScreen.kt
@Composable
fun TerminalScreen(
    onNavigateBack: () -> Unit,
    viewModel: TerminalViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TerminalEffect.NavigateBack -> onNavigateBack()
                is TerminalEffect.ScrollToBottom -> {
                    if (state.outputs.isNotEmpty()) {
                        listState.animateScrollToItem(state.outputs.lastIndex)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terminal") },
                actions = {
                    IconButton(onClick = {
                        viewModel.handleIntent(TerminalIntent.Disconnect)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, "Disconnect")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Terminal output
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f)
            ) {
                items(
                    items = state.outputs,
                    key = { it.id }
                ) { output ->
                    TerminalOutputItem(output)
                }
            }

            // Command input
            CommandInputRow(
                command = state.currentCommand,
                isFavorite = state.isCurrentCommandFavorite,
                onCommandChange = {
                    viewModel.handleIntent(TerminalIntent.UpdateCommand(it))
                },
                onSend = {
                    viewModel.handleIntent(TerminalIntent.ExecuteCommand)
                },
                onToggleFavorite = {
                    viewModel.handleIntent(TerminalIntent.ToggleCurrentCommandFavorite)
                }
            )
        }
    }
}
```

---

## 4. Dependency Injection

Проект использует **Hilt** для DI.

### 4.1 Dispatchers Module

```kotlin
// core/common/di/DispatchersModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}

// Qualifier annotations
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
```

### 4.2 Database Module

```kotlin
// data/di/DatabaseModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "terminalssh.db"
        ).build()
    }

    @Provides
    fun provideConnectionDao(database: AppDatabase): ConnectionDao {
        return database.connectionDao()
    }

    @Provides
    fun provideFavoriteCommandDao(database: AppDatabase): FavoriteCommandDao {
        return database.favoriteCommandDao()
    }
}
```

### 4.3 Data Module

```kotlin
// data/di/DataModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindSshRepository(impl: SshRepositoryImpl): SshRepository

    @Binds
    @Singleton
    abstract fun bindConnectionRepository(impl: ConnectionRepositoryImpl): ConnectionRepository

    @Binds
    @Singleton
    abstract fun bindFavoriteCommandRepository(
        impl: FavoriteCommandRepositoryImpl
    ): FavoriteCommandRepository
}
```

---

## 5. Навигация

Навигация реализована с помощью **Navigation Compose**.

```kotlin
// feature/terminal/navigation/TerminalNavigation.kt
const val TERMINAL_GRAPH_ROUTE = "terminal_graph"
const val CONNECT_ROUTE = "connect"
const val TERMINAL_ROUTE = "terminal/{connectionId}"

fun NavGraphBuilder.terminalGraph(navController: NavHostController) {
    navigation(
        startDestination = CONNECT_ROUTE,
        route = TERMINAL_GRAPH_ROUTE
    ) {
        composable(CONNECT_ROUTE) {
            ConnectScreen(
                onNavigateToTerminal = { connectionId ->
                    navController.navigate("terminal/$connectionId") {
                        popUpTo(CONNECT_ROUTE)
                    }
                }
            )
        }

        composable(
            route = TERMINAL_ROUTE,
            arguments = listOf(
                navArgument("connectionId") { type = NavType.LongType }
            )
        ) {
            TerminalScreen(
                onNavigateBack = {
                    navController.navigate(CONNECT_ROUTE) {
                        popUpTo(TERMINAL_ROUTE) { inclusive = true }
                    }
                }
            )
        }
    }
}
```

---

## 6. Управление состоянием (MVI)

### MVI Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                           UI (Compose)                          │
│  ┌─────────────────┐                    ┌────────────────────┐  │
│  │  User Action    │ ─── Intent ───────▶│    handleIntent()  │  │
│  └─────────────────┘                    └─────────┬──────────┘  │
│                                                   │             │
│                                                   ▼             │
│  ┌─────────────────┐                    ┌────────────────────┐  │
│  │  Render State   │ ◀── StateFlow ─────│   _state.update()  │  │
│  └─────────────────┘                    └────────────────────┘  │
│                                                                 │
│  ┌─────────────────┐                    ┌────────────────────┐  │
│  │  Handle Effect  │ ◀── Channel ───────│  _effect.send()    │  │
│  │  (Navigation)   │                    │  (one-time events) │  │
│  └─────────────────┘                    └────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Преимущества MVI в проекте

1. **Предсказуемость** — состояние изменяется только через Intent'ы
2. **Тестируемость** — легко тестировать редукцию State
3. **Отладка** — можно логировать все Intent'ы и State
4. **Time-travel debugging** — возможность воспроизвести состояние

---

## 7. Используемые библиотеки

| Библиотека | Версия | Назначение |
|------------|--------|------------|
| **Jetpack Compose** | BOM 2024.09.00 | UI Framework |
| **Material 3** | (via Compose BOM) | Design System |
| **Hilt** | 2.51.1 | Dependency Injection |
| **Room** | 2.6.1 | Локальная база данных |
| **Navigation Compose** | 2.8.4 | Навигация |
| **Apache MINA SSHD** | 2.14.0 | SSH клиент |
| **Kotlin Coroutines** | 1.9.0 | Асинхронность |
| **Lifecycle** | 2.10.0 | Lifecycle-aware компоненты |

### Версии Gradle Plugins

| Plugin | Версия |
|--------|--------|
| Android Gradle Plugin | 8.13.1 |
| Kotlin | 2.0.21 |
| KSP | 2.0.21-1.0.28 |

---

## 8. Потоки данных

### 8.1 Подключение к SSH

```
┌─────────────┐     ┌────────────────┐     ┌─────────────────┐
│ ConnectScreen│ ──▶│ ConnectViewModel│ ──▶│ ConnectSshUseCase│
└─────────────┘     └────────────────┘     └────────┬────────┘
                                                    │
                    ┌────────────────────────────────┘
                    ▼
┌─────────────────────────┐     ┌─────────────────┐
│   SshRepository         │ ──▶ │    SshClient    │
│ (SshRepositoryImpl)     │     │ (Apache MINA)   │
└─────────────────────────┘     └─────────────────┘
            │
            │ StateFlow<ConnectionState>
            ▼
┌─────────────────────────┐
│   ViewModel observes    │
│   connection state      │
└─────────────────────────┘
```

### 8.2 Выполнение команды

```
User Input ──▶ TerminalIntent.ExecuteCommand
                        │
                        ▼
              ┌─────────────────┐
              │ TerminalViewModel│
              └────────┬────────┘
                       │
                       ▼
              ┌─────────────────────┐
              │ ExecuteCommandUseCase│
              └────────┬────────────┘
                       │
                       ▼
              ┌─────────────────┐
              │  SshRepository  │
              └────────┬────────┘
                       │
                       ▼
              ┌─────────────────┐      ┌──────────────────┐
              │   SshClient     │ ───▶ │   SSH Server     │
              └────────┬────────┘      └────────┬─────────┘
                       │                        │
                       │◀───── Output ──────────┘
                       │
                       ▼
              ┌──────────────────────────┐
              │ MutableSharedFlow<Output>│
              └────────┬─────────────────┘
                       │
                       ▼
              ┌─────────────────┐
              │ ViewModel buffer│
              │ (100ms debounce)│
              └────────┬────────┘
                       │
                       ▼
              ┌─────────────────┐
              │ UI State Update │
              └─────────────────┘
```

### 8.3 Сохранение подключения

```
SaveConnectionUseCase
         │
         ▼
ConnectionRepository.save()
         │
         ▼
ConnectionRepositoryImpl
         │
         ▼
ConnectionDao.insert()
         │
         ▼
Room Database (SQLite)
```

---

## Заключение

Архитектура проекта TerminalSSH обеспечивает:

- **Масштабируемость** — легко добавлять новые функции
- **Тестируемость** — бизнес-логика изолирована от Android SDK
- **Поддерживаемость** — чёткое разделение ответственности
- **Производительность** — оптимизированная буферизация вывода терминала

Модульная структура позволяет:
- Параллельную разработку разных feature
- Инкрементальную сборку
- Переиспользование модулей