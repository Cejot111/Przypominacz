package com.kardiomed.app.data

import android.content.Context
import com.kardiomed.app.model.AlarmItem
import com.kardiomed.app.model.AlarmType
import com.kardiomed.app.model.ChecklistItem
import com.kardiomed.app.model.Note
import com.kardiomed.app.model.NoteType
import com.kardiomed.app.model.PressureEntry
import com.kardiomed.app.model.SoundPresets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AppRepository private constructor(private val context: Context) {

    private val notesMutex = Mutex()
    private val alarmsMutex = Mutex()

    private val _notesFlow = MutableStateFlow<List<Note>>(emptyList())
    val notesFlow: StateFlow<List<Note>> = _notesFlow.asStateFlow()

    private val _alarmsFlow = MutableStateFlow<List<AlarmItem>>(emptyList())
    val alarmsFlow: StateFlow<List<AlarmItem>> = _alarmsFlow.asStateFlow()

    private val notesFile: File get() = File(context.filesDir, "notes.json")
    private val alarmsFile: File get() = File(context.filesDir, "alarms.json")

    init {
        loadData()
    }

    private fun loadData() {
        // Wczytanie notatek
        if (!notesFile.exists()) {
            val initialNotes = createInitialNotes()
            saveNotesSync(initialNotes)
            _notesFlow.value = initialNotes
        } else {
            val notes = runCatching {
                val content = notesFile.readText()
                val array = JSONArray(content)
                val list = mutableListOf<Note>()
                for (i in 0 until array.length()) {
                    list.add(Note.fromJson(array.getJSONObject(i)))
                }
                list.sortedByDescending { it.updatedAt }
            }.getOrDefault(createInitialNotes())
            _notesFlow.value = notes
        }

        // Wczytanie alarmów
        if (!alarmsFile.exists()) {
            val initialAlarms = createInitialAlarms()
            saveAlarmsSync(initialAlarms)
            _alarmsFlow.value = initialAlarms
        } else {
            val alarms = runCatching {
                val content = alarmsFile.readText()
                val array = JSONArray(content)
                val list = mutableListOf<AlarmItem>()
                for (i in 0 until array.length()) {
                    list.add(AlarmItem.fromJson(array.getJSONObject(i)))
                }
                list.sortedWith(compareBy({ it.hour }, { it.minute }))
            }.getOrDefault(createInitialAlarms())
            _alarmsFlow.value = alarms
        }
    }

    // --- NOTATKI ---
    suspend fun saveNote(note: Note) = notesMutex.withLock {
        val current = _notesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == note.id }
        val updatedNote = note.copy(updatedAt = System.currentTimeMillis())
        if (index >= 0) {
            current[index] = updatedNote
        } else {
            current.add(0, updatedNote)
        }
        val sorted = current.sortedByDescending { it.updatedAt }
        saveNotesSync(sorted)
        _notesFlow.value = sorted
    }

    suspend fun deleteNote(noteId: String) = notesMutex.withLock {
        val filtered = _notesFlow.value.filter { it.id != noteId }
        saveNotesSync(filtered)
        _notesFlow.value = filtered
    }

    fun getNoteById(noteId: String): Note? {
        return _notesFlow.value.firstOrNull { it.id == noteId }
    }

    private fun saveNotesSync(notes: List<Note>) {
        val array = JSONArray()
        notes.forEach { array.put(it.toJson()) }
        notesFile.writeText(array.toString(2))
    }

    // --- ALARMY ---
    suspend fun saveAlarm(alarm: AlarmItem) = alarmsMutex.withLock {
        val current = _alarmsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == alarm.id }
        if (index >= 0) {
            current[index] = alarm
        } else {
            current.add(alarm)
        }
        val sorted = current.sortedWith(compareBy({ it.hour }, { it.minute }))
        saveAlarmsSync(sorted)
        _alarmsFlow.value = sorted
    }

    suspend fun updateAlarm(alarm: AlarmItem) = saveAlarm(alarm)

    suspend fun deleteAlarm(alarmId: String) = alarmsMutex.withLock {
        val filtered = _alarmsFlow.value.filter { it.id != alarmId }
        saveAlarmsSync(filtered)
        _alarmsFlow.value = filtered
    }

    fun getAlarms(): List<AlarmItem> = _alarmsFlow.value

    fun getAlarmById(alarmId: String): AlarmItem? = _alarmsFlow.value.firstOrNull { it.id == alarmId }

    private fun saveAlarmsSync(alarms: List<AlarmItem>) {
        val array = JSONArray()
        alarms.forEach { array.put(it.toJson()) }
        alarmsFile.writeText(array.toString(2))
    }

    // --- DANE POCZĄTKOWE ---
    private fun createInitialNotes(): List<Note> {
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val today = df.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = df.format(cal.time)

        val shoppingNote = Note(
            id = "default_shopping_list",
            title = "Lista do sklepu (Spożywcze & Dom)",
            type = NoteType.CHECKLIST,
            checklistItems = listOf(
                ChecklistItem(text = "🛒 Chleb żytni / pieczywo", isDone = false),
                ChecklistItem(text = "🥛 Mleko owsiane lub krowie", isDone = true),
                ChecklistItem(text = "🥚 Jajka (10 szt.)", isDone = false),
                ChecklistItem(text = "☕ Kawa ziarnista / mielona", isDone = false),
                ChecklistItem(text = "🍎 Jabłka / banany", isDone = false),
                ChecklistItem(text = "🧼 Kapsułki do prania / papier", isDone = false)
            )
        )

        val adhdChecklist = Note(
            id = "default_adhd_leaving",
            title = "Przed wyjściem z domu (Checklista ADHD)",
            type = NoteType.CHECKLIST,
            checklistItems = listOf(
                ChecklistItem(text = "🔑 Klucze do mieszkania i auta", isDone = false),
                ChecklistItem(text = "💳 Portfel / dokumenty / karta", isDone = false),
                ChecklistItem(text = "📱 Telefon naładowany", isDone = false),
                ChecklistItem(text = "💧 Butelka z wodą", isDone = false),
                ChecklistItem(text = "🔌 Wyłączona kuchenka, piekarnik i żelazko", isDone = false)
            )
        )

        val pressureNote = Note(
            id = "default_pressure_log",
            title = "Dziennik pomiarów ciśnienia i pulsu",
            type = NoteType.PRESSURE_TABLE,
            pressureEntries = listOf(
                PressureEntry(date = yesterday, slot = "Rano", sys = 122, dia = 78, pulse = 68, note = "Na czczo"),
                PressureEntry(date = yesterday, slot = "Wieczór", sys = 128, dia = 82, pulse = 72, note = "Przed snem"),
                PressureEntry(date = today, slot = "Rano", sys = 120, dia = 80, pulse = 69, note = "Prawidłowe")
            )
        )

        val brainDumpNote = Note(
            id = "default_brain_dump",
            title = "Zrzut myśli (Brain Dump ADHD)",
            type = NoteType.PLAIN_TEXT,
            textContent = "Szybkie zapiski zanim uciekną z głowy:\n- Zadzwonić do spółdzielni / mechanika w czwartek\n- Wymienić żarówkę w korytarzu\n- Zasada 5 minut: jeśli zadanie trwa mniej niż 5 minut, zrób je od razu!"
        )

        return listOf(shoppingNote, adhdChecklist, pressureNote, brainDumpNote)
    }

    private fun createInitialAlarms(): List<AlarmItem> {
        return listOf(
            AlarmItem(
                id = "alarm_morning_pressure",
                title = "Poranny pomiar ciśnienia + witaminy",
                type = AlarmType.PRESSURE,
                hour = 8,
                minute = 0,
                daysOfWeek = (1..7).toSet(),
                isEnabled = true,
                isRecurring = true,
                soundPreset = SoundPresets.CLASSIC_ALARM,
                vibrate = true,
                targetNoteId = "default_pressure_log"
            ),
            AlarmItem(
                id = "alarm_water_pomodoro",
                title = "Wypij szklankę wody + ruch",
                type = AlarmType.WATER,
                hour = 12,
                minute = 30,
                daysOfWeek = (1..7).toSet(),
                isEnabled = true,
                isRecurring = true,
                soundPreset = SoundPresets.GENTLE_CHIME,
                vibrate = true
            ),
            AlarmItem(
                id = "alarm_evening_pressure_meds",
                title = "Wieczorny pomiar + wyciszenie",
                type = AlarmType.PRESSURE,
                hour = 20,
                minute = 30,
                daysOfWeek = (1..7).toSet(),
                isEnabled = true,
                isRecurring = true,
                soundPreset = SoundPresets.DIGITAL_WAKE,
                vibrate = true,
                targetNoteId = "default_pressure_log"
            )
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: AppRepository? = null

        fun getInstance(context: Context): AppRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
