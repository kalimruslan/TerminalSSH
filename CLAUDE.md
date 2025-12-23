# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Language / Язык

**All responses must be in Russian (Все ответы должны быть на русском языке).**

## Project Overview

TerminalSSH is an Android SSH/SFTP client application built with Kotlin and Jetpack Compose. The project uses Material Design 3 and targets Android 7.0+ (API 24) through Android 15 (SDK 36).

### Key Features
- SSH terminal with command execution
- SFTP file browser
- Saved connections management
- Favorite commands per connection
- Command history with navigation
- Terminal settings (font size, color schemes)
- Password encryption (Android Keystore)
- Demo mode for testing without real server

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug

# Clean build
./gradlew clean
```

## Test Commands

```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Run all checks
./gradlew check
```

## Architecture

- **UI Layer**: Jetpack Compose with Material Design 3
- **Architecture Pattern**: Clean Architecture + MVI (Model-View-Intent)
- **Navigation**: Single Activity with Navigation Compose

### Clean Architecture + MVI Pattern

This project implements **Clean Architecture** with three distinct layers combined with **MVI (Model-View-Intent)** for state management:

**Layer Structure:**
- **Domain Layer** (`:domain`) - Pure Kotlin, no Android dependencies, contains business logic interfaces, models, and use cases
- **Data Layer** (`:data`) - Repository implementations, SSH/SFTP clients, Room database, DataStore, security
- **Presentation Layer** (feature modules) - Compose UI with ViewModels using MVI pattern

**Feature Modules:**
- `:feature:connect` - Connection screen
- `:feature:terminal` - SSH terminal screen
- `:feature:settings` - Settings screen
- `:feature:sftp` - SFTP file browser

**Key Technologies:**
- **UI:** Jetpack Compose with Material 3 + Material Icons Extended
- **DI:** Hilt (Dagger wrapper)
- **Navigation:** Jetpack Navigation Compose with callback pattern
- **SSH/SFTP:** Apache MINA SSHD 2.14.0
- **Database:** Room 2.6.1
- **Settings:** DataStore Preferences 1.1.1
- **Security:** Android Keystore (AES-256-GCM)
- **Async:** Kotlin Coroutines 1.9.0 + Flow

### Module Structure

```
TerminalSSH/
├── app/                    # Main app module (MainActivity + NavHost)
├── core/
│   ├── common/             # Utils, DI Dispatchers, Result wrapper
│   └── theme/              # Material 3 theme, terminal color schemes
├── domain/                 # Business logic (pure Kotlin)
│   ├── model/              # Domain models
│   ├── repository/         # Repository interfaces
│   └── usecase/            # Use cases
├── data/                   # Data layer
│   ├── database/           # Room (Entity, DAO, Database)
│   ├── di/                 # Hilt modules
│   ├── repository/         # Repository implementations
│   ├── security/           # PasswordEncryptor
│   ├── ssh/                # SshClient, DemoSshClient
│   └── sftp/               # SftpClient, DemoSftpClient
└── feature/
    ├── connect/            # Connection screen (MVI)
    ├── terminal/           # Terminal screen (MVI)
    ├── settings/           # Settings screen (MVI)
    └── sftp/               # SFTP browser (MVI)
```

### Key Directories

- `app/src/main/java/com/ruslan/terminalssh/` - Main activity and navigation
- `domain/src/main/java/.../model/` - Domain models (ConnectionConfig, FileEntry, etc.)
- `domain/src/main/java/.../usecase/` - Use cases for business operations
- `data/src/main/java/.../ssh/` - SSH client implementation
- `data/src/main/java/.../sftp/` - SFTP client implementation
- `feature/*/src/main/java/.../` - Feature-specific screens and ViewModels
- `gradle/libs.versions.toml` - Centralized dependency version management

### Entry Point

`MainActivity.kt` - Single activity using `ComponentActivity` with edge-to-edge display. Contains NavHost with routes:
- `connect` - Connection screen (start destination)
- `terminal/{connectionId}` - Terminal screen
- `settings` - Settings screen
- `sftp` - SFTP file browser

## Build Configuration

- **Gradle**: Kotlin DSL (`build.gradle.kts`)
- **Java/Kotlin Target**: Java 11
- **Compose**: Enabled via Kotlin Compose plugin
- **Namespace**: `com.ruslan.terminalssh`

### MVI Contract Pattern

Each feature screen has a Contract file with:
```kotlin
// State - current screen state
data class ExampleState(...)

// Intent - user actions
sealed class ExampleIntent {
    data class Action(val param: String) : ExampleIntent()
}

// Effect - one-time events (navigation, snackbar)
sealed class ExampleEffect {
    data object NavigateBack : ExampleEffect()
}
```

### Result Wrapper

All repository operations use `Result<T>` sealed class:
```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}
```

### Repository Flow
```
Use Cases → Repository Interface → Repository Implementation → Data Source (SSH/Room/DataStore)
```

### Navigation Pattern

Feature modules expose navigation functions via callback pattern:
```kotlin
// In feature module
fun NavGraphBuilder.featureScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOther: () -> Unit
)

// In app module (MainActivity)
featureScreen(
    onNavigateBack = { navController.popBackStack() },
    onNavigateToOther = { navController.navigate(OTHER_ROUTE) }
)
```
