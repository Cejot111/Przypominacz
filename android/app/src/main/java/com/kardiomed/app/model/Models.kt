package com.kardiomed.app.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class NoteType {
    PRESSURE_TABLE,
    CHECKLIST,
    PLAIN_TEXT
}

enum class AlarmType(val labelPl: String) {
    PRESSURE("Pomiar ciśnienia"),
    MEDICATION("Zażycie leków"),
    CUSTOM("Własne przypomnienie")
}

object SoundPresets {
    const val SYSTEM_DEFAULT = "SYSTEM_DEFAULT"
    const val CLASSIC_ALARM = "CLASSIC_ALARM"
    const val MEDICAL_BEEP = "MEDICAL_BEEP"
    const val GENTLE_CHIME = "GENTLE_CHIME"
    const val DIGITAL_WAKE = "DIGITAL_WAKE"
    const val CUSTOM_SYSTEM = "CUSTOM_SYSTEM"

    val ALL = listOf(
        SYSTEM_DEFAULT to "Domyślny dzwonek budzika",
        CLASSIC_ALARM to "Klasyczny energiczny budzik",
        MEDICAL_BEEP to "Sygnał medyczny (pulsometr)",
        GENTLE_CHIME to "Łagodny gong / dzwonki",
        DIGITAL_WAKE to "Cyfrowy dźwięk elektroniczny",
        CUSTOM_SYSTEM to "Wybierz z telefonu (własny dźwięk)"
    )

    fun getDisplayName(key: String): String {
        return ALL.firstOrNull { it.first == key }?.second ?: "Domyślny dźwięk"
    }
}

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isDone: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("text", text)
        put("isDone", isDone)
    }

    companion object {
        fun fromJson(json: JSONObject): ChecklistItem = ChecklistItem(
            id = json.optString("id", UUID.randomUUID().toString()),
            text = json.optString("text", ""),
            isDone = json.optBoolean("isDone", false)
        )
    }
}

data class PressureEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: String,            // YYYY-MM-DD
    val slot: String,            // "Rano", "Wieczór", "Południe" lub własne
    val sys: Int? = null,        // Ciśnienie skurczowe (np. 120)
    val dia: Int? = null,        // Ciśnienie rozkurczowe (np. 80)
    val pulse: Int? = null,      // Tętno (np. 72)
    val note: String = ""        // Uwagi (np. po kawie, spacer)
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("date", date)
        put("slot", slot)
        sys?.let { put("sys", it) }
        dia?.let { put("dia", it) }
        pulse?.let { put("pulse", it) }
        put("note", note)
    }

    companion object {
        fun fromJson(json: JSONObject): PressureEntry = PressureEntry(
            id = json.optString("id", UUID.randomUUID().toString()),
            date = json.optString("date", ""),
            slot = json.optString("slot", "Rano"),
            sys = if (json.has("sys") && !json.isNull("sys")) json.optInt("sys") else null,
            dia = if (json.has("dia") && !json.isNull("dia")) json.optInt("dia") else null,
            pulse = if (json.has("pulse") && !json.isNull("pulse")) json.optInt("pulse") else null,
            note = json.optString("note", "")
        )
    }
}

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: NoteType,
    val textContent: String = "",
    val checklistItems: List<ChecklistItem> = emptyList(),
    val pressureEntries: List<PressureEntry> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("type", type.name)
        put("textContent", textContent)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)

        val checkArray = JSONArray()
        checklistItems.forEach { checkArray.put(it.toJson()) }
        put("checklistItems", checkArray)

        val pressArray = JSONArray()
        pressureEntries.forEach { pressArray.put(it.toJson()) }
        put("pressureEntries", pressArray)
    }

    companion object {
        fun fromJson(json: JSONObject): Note {
            val checkList = mutableListOf<ChecklistItem>()
            val checkArray = json.optJSONArray("checklistItems")
            if (checkArray != null) {
                for (i in 0 until checkArray.length()) {
                    checkList.add(ChecklistItem.fromJson(checkArray.getJSONObject(i)))
                }
            }

            val pressList = mutableListOf<PressureEntry>()
            val pressArray = json.optJSONArray("pressureEntries")
            if (pressArray != null) {
                for (i in 0 until pressArray.length()) {
                    pressList.add(PressureEntry.fromJson(pressArray.getJSONObject(i)))
                }
            }

            return Note(
                id = json.optString("id", UUID.randomUUID().toString()),
                title = json.optString("title", "Bez tytułu"),
                type = runCatching { NoteType.valueOf(json.optString("type", NoteType.PLAIN_TEXT.name)) }
                    .getOrDefault(NoteType.PLAIN_TEXT),
                textContent = json.optString("textContent", ""),
                checklistItems = checkList,
                pressureEntries = pressList,
                createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
            )
        }
    }
}

data class AlarmItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: AlarmType,
    val hour: Int,
    val minute: Int,
    val daysOfWeek: Set<Int> = emptySet(), // 1=Pn, 2=Wt, 3=Śr, 4=Cz, 5=Pt, 6=Sb, 7=Nd (empty = jednorazowy)
    val isEnabled: Boolean = true,
    val isRecurring: Boolean = false,
    val soundPreset: String = SoundPresets.CLASSIC_ALARM,
    val customSoundUri: String? = null,
    val vibrate: Boolean = true,
    val targetNoteId: String? = null
) {
    fun timeFormatted(): String = String.format("%02d:%02d", hour, minute)

    fun daysFormatted(): String {
        if (!isRecurring || daysOfWeek.isEmpty()) return "Jednorazowy"
        if (daysOfWeek.size == 7) return "Codziennie"
        if (daysOfWeek == setOf(1, 2, 3, 4, 5)) return "Dni robocze (Pn-Pt)"
        if (daysOfWeek == setOf(6, 7)) return "Weekend (Sb-Nd)"
        val names = mapOf(1 to "Pn", 2 to "Wt", 3 to "Śr", 4 to "Cz", 5 to "Pt", 6 to "Sb", 7 to "Nd")
        return daysOfWeek.sorted().mapNotNull { names[it] }.joinToString(", ")
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("type", type.name)
        put("hour", hour)
        put("minute", minute)
        put("isEnabled", isEnabled)
        put("isRecurring", isRecurring)
        put("soundPreset", soundPreset)
        customSoundUri?.let { put("customSoundUri", it) }
        put("vibrate", vibrate)
        targetNoteId?.let { put("targetNoteId", it) }
        val daysArray = JSONArray()
        daysOfWeek.forEach { daysArray.put(it) }
        put("daysOfWeek", daysArray)
    }

    companion object {
        fun fromJson(json: JSONObject): AlarmItem {
            val days = mutableSetOf<Int>()
            val daysArray = json.optJSONArray("daysOfWeek")
            if (daysArray != null) {
                for (i in 0 until daysArray.length()) {
                    days.add(daysArray.getInt(i))
                }
            }
            return AlarmItem(
                id = json.optString("id", UUID.randomUUID().toString()),
                title = json.optString("title", "Przypomnienie"),
                type = runCatching { AlarmType.valueOf(json.optString("type", AlarmType.PRESSURE.name)) }
                    .getOrDefault(AlarmType.PRESSURE),
                hour = json.optInt("hour", 8),
                minute = json.optInt("minute", 0),
                daysOfWeek = days,
                isEnabled = json.optBoolean("isEnabled", true),
                isRecurring = json.optBoolean("isRecurring", false),
                soundPreset = json.optString("soundPreset", SoundPresets.CLASSIC_ALARM),
                customSoundUri = if (json.has("customSoundUri")) json.optString("customSoundUri") else null,
                vibrate = json.optBoolean("vibrate", true),
                targetNoteId = if (json.has("targetNoteId")) json.optString("targetNoteId") else null
            )
        }
    }
}
