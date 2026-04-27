package dev.shinyparadise.sast.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dev.shinyparadise.sast.domain.AppSettings
import dev.shinyparadise.sast.domain.AIMode
import dev.shinyparadise.sast.domain.SettingsKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val dataStore = context.dataStore

    val settingsFlow: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            aiAnalysisEnabled = preferences[SettingsKeys.AI_ANALYSIS_ENABLED] ?: false,
            aiAnalysisMode = preferences[SettingsKeys.AI_ANALYSIS_MODE]?.let {
                try { AIMode.valueOf(it) } catch (e: Exception) { AIMode.ON_DEVICE }
            } ?: AIMode.ON_DEVICE,
            remoteModelUrl = preferences[SettingsKeys.REMOTE_MODEL_URL],
            remoteApiKey = preferences[SettingsKeys.REMOTE_API_KEY],
            selectedRemoteModel = preferences[SettingsKeys.SELECTED_REMOTE_MODEL],
        )
    }

    suspend fun updateSettings(settings: AppSettings) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.AI_ANALYSIS_ENABLED] = settings.aiAnalysisEnabled
            preferences[SettingsKeys.AI_ANALYSIS_MODE] = settings.aiAnalysisMode.name
            settings.remoteModelUrl?.let {
                preferences[SettingsKeys.REMOTE_MODEL_URL] = it
            } ?: preferences.remove(SettingsKeys.REMOTE_MODEL_URL)
            settings.remoteApiKey?.let {
                preferences[SettingsKeys.REMOTE_API_KEY] = it
            } ?: preferences.remove(SettingsKeys.REMOTE_API_KEY)
            settings.selectedRemoteModel?.let {
                preferences[SettingsKeys.SELECTED_REMOTE_MODEL] = it
            } ?: preferences.remove(SettingsKeys.SELECTED_REMOTE_MODEL)
        }
    }
}
