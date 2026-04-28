package dev.shinyparadise.sast.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.shinyparadise.sast.domain.AIMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки AI") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SettingsSectionHeader("AI Анализ")

            SettingsToggleItem(
                title = "Включить AI анализ",
                subtitle = "Автоматический анализ уязвимостей с помощью AI",
                checked = settings.aiAnalysisEnabled,
                onCheckedChange = viewModel::updateAiAnalysisEnabled
            )

            if (settings.aiAnalysisEnabled) {
                HorizontalDivider()

                SettingsSectionHeader("Режим AI")

                SettingsRadioItem(
                    title = "On-Device (локальный)",
                    subtitle = "Эвристический анализ на устройстве",
                    selected = settings.aiAnalysisMode == AIMode.ON_DEVICE,
                    onClick = { viewModel.updateAiAnalysisMode(AIMode.ON_DEVICE) }
                )

                SettingsRadioItem(
                    title = "Remote (удалённый)",
                    subtitle = "Использовать удалённую AI модель",
                    selected = settings.aiAnalysisMode == AIMode.REMOTE,
                    onClick = { viewModel.updateAiAnalysisMode(AIMode.REMOTE) }
                )

                if (settings.aiAnalysisMode == AIMode.REMOTE) {
                    HorizontalDivider()

                    SettingsSectionHeader("Параметры API")

                    SettingsTextFieldItem(
                        label = "URL модели",
                        value = settings.remoteModelUrl ?: "",
                        onValueChange = viewModel::updateRemoteModelUrl,
                        placeholder = "https://api.example.com/v1/chat/completions",
                        keyboardType = KeyboardType.Uri
                    )

                    SettingsTextFieldItem(
                        label = "API Key",
                        value = settings.remoteApiKey ?: "",
                        onValueChange = viewModel::updateRemoteApiKey,
                        placeholder = "sk-...",
                        keyboardType = KeyboardType.Password,
                        isPassword = true
                    )

                    SettingsTextFieldItem(
                        label = "Модель",
                        value = settings.selectedRemoteModel ?: "",
                        onValueChange = viewModel::updateSelectedRemoteModel,
                        placeholder = "gpt-4o-mini",
                        keyboardType = KeyboardType.Text
                    )

                    HorizontalDivider()

                    SettingsSectionHeader("Chunk Size")

                    SettingsSliderItem(
                        label = "Размер чанка",
                        subtitle = "Уязвимостей на запрос: ${settings.aiChunkSize}",
                        value = settings.aiChunkSize.toFloat(),
                        valueRange = 5f..50f,
                        steps = 8,
                        onValueChange = { viewModel.updateAiChunkSize(it.toInt()) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsRadioItem(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = selected,
            onCheckedChange = { onClick() }
        )
    }
}

@Composable
private fun SettingsTextFieldItem(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None
        )
    }
}

@Composable
private fun SettingsSliderItem(
    label: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}