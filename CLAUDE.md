# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Language / Язык

**All responses must be in Russian (Все ответы должны быть на русском языке).**

## Project Overview

TerminalSSH is an Android application built with Kotlin and Jetpack Compose. The project uses Material Design 3 and targets Android 7.0+ (API 24) through Android 15 (SDK 36).

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
- **Architecture Pattern**: Single Activity with Compose navigation
- **Theme System**: Dynamic colors on Android 12+, fallback palette for older versions

### Clean Architecture + MVI Pattern

This project implements **Clean Architecture** with three distinct layers combined with **MVI (Model-View-Intent)** for state management:

**Layer Structure:**
- **Domain Layer** (`:domain`) - Pure Kotlin, no Android dependencies, contains business logic interfaces and models
- **Data Layer** (`:data`) - Repository implementations, API clients (Retrofit), data sources, and mappers
- **Presentation Layer** (feature modules) - Compose UI with ViewModels using MVI pattern

**Key Technologies:**
- **UI:** Jetpack Compose with Material 3
- **DI:** Hilt (Dagger wrapper)
- **Navigation:** Jetpack Navigation Compose with FeatureApi pattern
- **Networking:** Retrofit 2 + OkHttp3 + Gson
- **Async:** Kotlin Coroutines + Flow
- **Storage:** EncryptedSharedPreferences (Room prepared but not implemented)

### Key Directories

- `app/src/main/java/com/ruslan/terminalssh/` - Kotlin source code
- `app/src/main/java/com/ruslan/terminalssh/ui/theme/` - Compose theming (colors, typography, theme config)
- `app/src/main/res/` - Android resources (strings, drawables, XML configs)
- `gradle/libs.versions.toml` - Centralized dependency version management

### Entry Point

`MainActivity.kt` - Single activity using `ComponentActivity` with edge-to-edge display enabled.

## Build Configuration

- **Gradle**: Kotlin DSL (`build.gradle.kts`)
- **Java/Kotlin Target**: Java 11
- **Compose**: Enabled via Kotlin Compose plugin
- **Namespace**: `com.ruslan.terminalssh`

### Data Layer Pattern

**NetworkResult Wrapper:**
All API responses use `NetworkResult<T>` sealed interface:
```kotlin
sealed interface NetworkResult<out T> {
    class Success<out T>(val data: T)
    class Error<out T>(val errorCode: Int, val message: String?)
    class Loading<out T>()
}
```

**Repository Flow:**
```
API Client (Retrofit) → AppDataSource → Mapper → Repository [NetworkResult] → Use Cases → ViewModels → UI