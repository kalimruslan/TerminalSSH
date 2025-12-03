package com.ruslan.terminalssh.feature.terminal.terminal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ruslan.terminalssh.core.theme.TerminalColors
import com.ruslan.terminalssh.domain.model.FavoriteCommand
import com.ruslan.terminalssh.domain.model.OutputType
import com.ruslan.terminalssh.domain.model.TerminalOutput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    onNavigateBack: () -> Unit,
    viewModel: TerminalViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

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
        containerColor = TerminalColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Terminal", color = TerminalColors.Text) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TerminalColors.Background
                ),
                actions = {
                    IconButton(onClick = { viewModel.handleIntent(TerminalIntent.Disconnect) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Disconnect",
                            tint = TerminalColors.Error
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
                onToggleFavorite = { viewModel.handleIntent(TerminalIntent.ToggleCurrentCommandFavorite) },
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
                .background(TerminalColors.Background)
        ) {
            FavoritesDropdown(
                favoriteCommands = state.favoriteCommands,
                isExpanded = state.showFavoritesDropdown,
                onToggle = { viewModel.handleIntent(TerminalIntent.ToggleFavoritesDropdown) },
                onSelectCommand = { viewModel.handleIntent(TerminalIntent.SelectFavoriteCommand(it)) },
                modifier = Modifier.padding(horizontal = 8.dp)
            )

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
                    TerminalOutputItem(output)
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
    modifier: Modifier = Modifier
) {
    if (favoriteCommands.isEmpty()) return

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(TerminalColors.Background.copy(alpha = 0.8f))
                .clickable { onToggle() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = TerminalColors.Prompt
            )
            Text(
                text = "Favorites (${favoriteCommands.size})",
                color = TerminalColors.Text,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            Icon(
                if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = TerminalColors.Text
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = TerminalColors.Background.copy(alpha = 0.95f)
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
                                color = TerminalColors.Command,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
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

@Composable
private fun TerminalOutputItem(output: TerminalOutput) {
    val color = when (output.type) {
        OutputType.COMMAND -> TerminalColors.Command
        OutputType.OUTPUT -> TerminalColors.Text
        OutputType.ERROR -> TerminalColors.Error
    }

    Text(
        text = output.text,
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        modifier = Modifier.padding(vertical = 1.dp)
    )
}

@Composable
private fun CommandInput(
    command: String,
    onCommandChange: (String) -> Unit,
    onExecute: () -> Unit,
    isFavorite: Boolean,
    showFavoriteButton: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalColors.Background)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$ ",
            color = TerminalColors.Prompt,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )

        TextField(
            value = command,
            onValueChange = onCommandChange,
            modifier = Modifier.weight(1f),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = TerminalColors.Text,
                unfocusedTextColor = TerminalColors.Text,
                cursorColor = TerminalColors.Prompt,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            textStyle = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            ),
            minLines = 1,
            maxLines = 5,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Default,
                keyboardType = KeyboardType.Text
            ),
            placeholder = {
                Text(
                    "Enter command...",
                    color = TerminalColors.Text.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            }
        )

        if (showFavoriteButton) {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) TerminalColors.Prompt else TerminalColors.Text.copy(alpha = 0.5f)
                )
            }
        }

        IconButton(onClick = onExecute) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Execute",
                tint = TerminalColors.Prompt
            )
        }
    }
}
