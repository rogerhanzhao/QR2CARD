package com.calb.qr2card.data

import android.content.Context
import kotlinx.serialization.json.Json

class TemplateRepository(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    },
) {
    fun load(context: Context): TemplateConfig {
        return runCatching {
            context.assets.open("template_config.json").bufferedReader().use { reader ->
                json.decodeFromString<TemplateConfig>(reader.readText())
            }
        }.getOrDefault(TemplateConfig())
    }
}
