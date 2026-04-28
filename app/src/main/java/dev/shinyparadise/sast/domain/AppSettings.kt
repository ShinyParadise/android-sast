package dev.shinyparadise.sast.domain

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

data class AppSettings(
    val aiAnalysisEnabled: Boolean = false,
    val aiAnalysisMode: AIMode = AIMode.ON_DEVICE,
    val remoteModelUrl: String? = null,
    val remoteApiKey: String? = null,
    val selectedRemoteModel: String? = null,
    val aiChunkSize: Int = 20,
)

enum class AIMode {
    ON_DEVICE,
    REMOTE
}

object SettingsKeys {
    val AI_ANALYSIS_ENABLED = booleanPreferencesKey("ai_analysis_enabled")
    val AI_ANALYSIS_MODE = stringPreferencesKey("ai_analysis_mode")
    val REMOTE_MODEL_URL = stringPreferencesKey("remote_model_url")
    val REMOTE_API_KEY = stringPreferencesKey("remote_api_key")
    val SELECTED_REMOTE_MODEL = stringPreferencesKey("selected_remote_model")
    val AI_CHUNK_SIZE = intPreferencesKey("ai_chunk_size")
}
