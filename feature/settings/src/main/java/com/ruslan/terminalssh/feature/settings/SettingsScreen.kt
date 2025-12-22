package com.ruslan.terminalssh.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ruslan.terminalssh.core.theme.TerminalColors
import com.ruslan.terminalssh.domain.model.ColorScheme
import com.ruslan.terminalssh.feature.settings.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Font Size Section
            Text(
                text = stringResource(R.string.settings_font_size),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            FontSizeSelector(
                currentSize = state.fontSize,
                onSizeChange = { viewModel.handleIntent(SettingsIntent.SetFontSize(it)) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Color Scheme Section
            Text(
                text = stringResource(R.string.settings_color_scheme),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            ColorSchemeSelector(
                currentScheme = state.colorScheme,
                onSchemeChange = { viewModel.handleIntent(SettingsIntent.SetColorScheme(it)) }
            )
        }
    }
}

@Composable
private fun FontSizeSelector(
    currentSize: Int,
    onSizeChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_size_format, currentSize),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.settings_preview),
                    fontFamily = FontFamily.Monospace,
                    fontSize = currentSize.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = currentSize.toFloat(),
                onValueChange = { onSizeChange(it.toInt()) },
                valueRange = 10f..24f,
                steps = 13
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("10", style = MaterialTheme.typography.bodySmall)
                Text("24", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ColorSchemeSelector(
    currentScheme: ColorScheme,
    onSchemeChange: (ColorScheme) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ColorSchemeItem(
            name = stringResource(R.string.color_scheme_dark),
            scheme = ColorScheme.DARK,
            colors = TerminalColors.Dark,
            isSelected = currentScheme == ColorScheme.DARK,
            onClick = { onSchemeChange(ColorScheme.DARK) }
        )

        ColorSchemeItem(
            name = stringResource(R.string.color_scheme_light),
            scheme = ColorScheme.LIGHT,
            colors = TerminalColors.Light,
            isSelected = currentScheme == ColorScheme.LIGHT,
            onClick = { onSchemeChange(ColorScheme.LIGHT) }
        )

        ColorSchemeItem(
            name = stringResource(R.string.color_scheme_solarized_dark),
            scheme = ColorScheme.SOLARIZED_DARK,
            colors = TerminalColors.SolarizedDark,
            isSelected = currentScheme == ColorScheme.SOLARIZED_DARK,
            onClick = { onSchemeChange(ColorScheme.SOLARIZED_DARK) }
        )

        ColorSchemeItem(
            name = stringResource(R.string.color_scheme_monokai),
            scheme = ColorScheme.MONOKAI,
            colors = TerminalColors.Monokai,
            isSelected = currentScheme == ColorScheme.MONOKAI,
            onClick = { onSchemeChange(ColorScheme.MONOKAI) }
        )
    }
}

@Composable
private fun ColorSchemeItem(
    name: String,
    scheme: ColorScheme,
    colors: com.ruslan.terminalssh.core.theme.TerminalColorScheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color preview
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.background)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$ ls",
                        color = colors.prompt,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "out",
                        color = colors.text,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = stringResource(R.string.settings_selected),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
