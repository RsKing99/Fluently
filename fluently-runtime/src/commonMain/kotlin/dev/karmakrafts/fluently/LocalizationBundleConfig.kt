package dev.karmakrafts.fluently

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class LocalizationBundleConfig(
    val languages: Map<String, Entry> = emptyMap()
) {
    @Serializable
    data class Entry(
        @SerialName("display_name") val displayName: String,
        val path: String
    )

    companion object {
        const val VERSION: Int = 1

        @OptIn(ExperimentalSerializationApi::class)
        private val json: Json = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            prettyPrintIndent = "\t"
        }

        fun load(source: Source): LocalizationBundleConfig = json.decodeFromString(source.readString())
    }

    fun save(sink: Sink) = sink.writeString(json.encodeToString(this))
}