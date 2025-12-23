package com.ruslan.terminalssh.feature.terminal.terminal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.Alignment
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ruslan.terminalssh.feature.terminal.R
import com.ruslan.terminalssh.core.theme.TerminalColors
import com.ruslan.terminalssh.core.theme.TerminalColorScheme
import com.ruslan.terminalssh.domain.model.ColorScheme
import com.ruslan.terminalssh.domain.model.FavoriteCommand
import com.ruslan.terminalssh.domain.model.OutputType
import com.ruslan.terminalssh.domain.model.TerminalOutput

private fun ColorScheme.toTerminalColors(): TerminalColorScheme = when (this) {
    ColorScheme.DARK -> TerminalColors.Dark
    ColorScheme.LIGHT -> TerminalColors.Light
    ColorScheme.SOLARIZED_DARK -> TerminalColors.SolarizedDark
    ColorScheme.MONOKAI -> TerminalColors.Monokai
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSftp: () -> Unit,
    viewModel: TerminalViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val colors = state.colorScheme.toTerminalColors()
    val fontSize = state.fontSize

    // Устанавливаем светлые иконки статус-бара для темного фона терминала
    DisposableEffect(Unit) {
        val activity = context as? ComponentActivity
        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        onDispose {
            // Возвращаем стандартный стиль при выходе с экрана
            activity?.enableEdgeToEdge()
        }
    }

    // Обработка навигации
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TerminalEffect.NavigateBack -> onNavigateBack()
                is TerminalEffect.ScrollToBottom -> { /* Обрабатывается ниже через snapshotFlow */ }
            }
        }
    }

    // Автоскролл при изменении outputs - решает race condition при открытии экрана
    LaunchedEffect(listState) {
        snapshotFlow { state.outputs.size }
            .distinctUntilChanged()
            .collect { size ->
                if (size > 0) {
                    listState.animateScrollToItem(size - 1)
                }
            }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.terminal_title), color = colors.text) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background
                ),
                actions = {
                    IconButton(onClick = onNavigateToSftp) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = stringResource(R.string.terminal_files),
                            tint = colors.text
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.terminal_settings),
                            tint = colors.text
                        )
                    }
                    IconButton(onClick = { viewModel.handleIntent(TerminalIntent.Disconnect) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.terminal_disconnect),
                            tint = colors.error
                        )
                    }
                }
            )
        },
        bottomBar = {
            CommandInput(
                command = state.currentCommand,
                onCommandChange = { viewModel.handleIntent(TerminalIntent.UpdateCommand(it)) },
                onExecute = { viewModel.handleIntent(TerminalIntent.ExecuteCommand) },
                isFavorite = state.isCurrentCommandFavorite,
                showFavoriteButton = state.connectionId > 0,
                showHistoryButtons = state.connectionId > 0,
                historyIndex = state.historyIndex,
                onToggleFavorite = { viewModel.handleIntent(TerminalIntent.ToggleCurrentCommandFavorite) },
                onHistoryUp = { viewModel.handleIntent(TerminalIntent.HistoryUp) },
                onHistoryDown = { viewModel.handleIntent(TerminalIntent.HistoryDown) },
                colors = colors,
                fontSize = fontSize,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(8.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colors.background)
        ) {
            FavoritesDropdown(
                favoriteCommands = state.favoriteCommands,
                isExpanded = state.showFavoritesDropdown,
                onToggle = { viewModel.handleIntent(TerminalIntent.ToggleFavoritesDropdown) },
                onSelectCommand = { viewModel.handleIntent(TerminalIntent.SelectFavoriteCommand(it)) },
                colors = colors,
                fontSize = fontSize,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            val clipboardManager = LocalClipboardManager.current

            SelectionContainer {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    state = listState
                ) {
                    items(
                        items = state.outputs,
                        key = { it.id }
                    ) { output ->
                        TerminalOutputItem(
                            output = output,
                            colors = colors,
                            fontSize = fontSize,
                            onLongClick = {
                                clipboardManager.setText(AnnotatedString(output.text))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritesDropdown(
    favoriteCommands: List<FavoriteCommand>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSelectCommand: (FavoriteCommand) -> Unit,
    colors: TerminalColorScheme,
    fontSize: Int,
    modifier: Modifier = Modifier
) {
    if (favoriteCommands.isEmpty()) return

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.background.copy(alpha = 0.8f))
                .clickable { onToggle() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = colors.prompt
            )
            Text(
                text = stringResource(R.string.terminal_favorites, favoriteCommands.size),
                color = colors.text,
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            Icon(
                if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
                contentDescription = if (isExpanded) stringResource(R.string.terminal_collapse) else stringResource(R.string.terminal_expand),
                tint = colors.text
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.background.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column {
                    favoriteCommands.forEach { command ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectCommand(command) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = command.command,
                                color = colors.command,
                                fontFamily = FontFamily.Monospace,
                                fontSize = (fontSize - 1).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TerminalOutputItem(
    output: TerminalOutput,
    colors: TerminalColorScheme,
    fontSize: Int,
    onLongClick: () -> Unit
) {
    val color = when (output.type) {
        OutputType.COMMAND -> colors.command
        OutputType.OUTPUT -> colors.text
        OutputType.ERROR -> colors.error
    }

    Text(
        text = output.text,
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize.sp,
        lineHeight = (fontSize + 4).sp,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            )
            .padding(vertical = 1.dp)
    )
}

@Composable
private fun CommandInput(
    command: String,
    onCommandChange: (String) -> Unit,
    onExecute: () -> Unit,
    isFavorite: Boolean,
    showFavoriteButton: Boolean,
    showHistoryButtons: Boolean,
    historyIndex: Int,
    onToggleFavorite: () -> Unit,
    onHistoryUp: () -> Unit,
    onHistoryDown: () -> Unit,
    colors: TerminalColorScheme,
    fontSize: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$ ",
            color = colors.prompt,
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize.sp
        )

        TextField(
            value = command,
            onValueChange = onCommandChange,
            modifier = Modifier.weight(1f),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text,
                cursorColor = colors.prompt,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            textStyle = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp,
                lineHeight = (fontSize + 2).sp
            ),
            minLines = 1,
            maxLines = 5,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Default,
                keyboardType = KeyboardType.Text
            ),
            placeholder = {
                Text(
                    stringResource(R.string.terminal_enter_command),
                    color = colors.text.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize.sp
                )
            }
        )

        if (showHistoryButtons) {
            Column {
                IconButton(onClick = onHistoryUp) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.terminal_previous_command),
                        tint = colors.text.copy(alpha = 0.7f)
                    )
                }
                IconButton(onClick = onHistoryDown) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.terminal_next_command),
                        tint = if (historyIndex >= 0) colors.text.copy(alpha = 0.7f)
                               else colors.text.copy(alpha = 0.3f)
                    )
                }
            }
        }

        if (showFavoriteButton) {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = if (isFavorite) stringResource(R.string.terminal_remove_from_favorites) else stringResource(R.string.terminal_add_to_favorites),
                    tint = if (isFavorite) colors.prompt else colors.text.copy(alpha = 0.5f)
                )
            }
        }

        IconButton(onClick = onExecute) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.terminal_execute),
                tint = colors.prompt
            )
        }
    }
}
