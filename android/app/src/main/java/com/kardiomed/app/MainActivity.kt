package com.kardiomed.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.kardiomed.app.alarm.AlarmActivity
import com.kardiomed.app.alarm.AlarmScheduler
import com.kardiomed.app.alarm.AlarmService
import com.kardiomed.app.data.AppRepository
import com.kardiomed.app.model.AlarmItem
import com.kardiomed.app.model.Note
import com.kardiomed.app.model.NoteType
import com.kardiomed.app.ui.AlarmsScreen
import com.kardiomed.app.ui.KardioMedTheme
import com.kardiomed.app.ui.NoteDetailScreen
import com.kardiomed.app.ui.NotesScreen
import com.kardiomed.app.ui.StatsScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: AppRepository

    // Obsługa uprawnień powiadomień na Androidzie 13+
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Uprawnienie przyznane lub odrzucone */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = AppRepository.getInstance(this)

        requestPermissionsIfNeeded()

        val navigateTo = intent.getStringExtra("NAVIGATE_TO")
        val targetNoteId = intent.getStringExtra("TARGET_NOTE_ID")
        val autoAddRow = intent.getBooleanExtra("AUTO_ADD_ROW", false)

        setContent {
            KardioMedTheme {
                MainAppRoot(
                    repository = repository,
                    initialNavigateTo = navigateTo,
                    initialTargetNoteId = targetNoteId,
                    autoAddRow = autoAddRow,
                    onTestAlarm = { alarm ->
                        // Uruchomienie natychmiastowego testu alarmu
                        val testIntent = Intent(this, AlarmService::class.java).apply {
                            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
                            putExtra(AlarmScheduler.EXTRA_ALARM_TITLE, alarm.title)
                            putExtra(AlarmScheduler.EXTRA_ALARM_TYPE, alarm.type.name)
                            putExtra(AlarmScheduler.EXTRA_SOUND_PRESET, alarm.soundPreset)
                            putExtra(AlarmScheduler.EXTRA_CUSTOM_SOUND_URI, alarm.customSoundUri)
                            putExtra(AlarmScheduler.EXTRA_VIBRATE, alarm.vibrate)
                            putExtra(AlarmScheduler.EXTRA_TARGET_NOTE_ID, alarm.targetNoteId)
                        }
                        ContextCompat.startForegroundService(this, testIntent)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun MainAppRoot(
    repository: AppRepository,
    initialNavigateTo: String?,
    initialTargetNoteId: String?,
    autoAddRow: Boolean,
    onTestAlarm: (AlarmItem) -> Unit
) {
    val notes by repository.notesFlow.collectAsState()
    val alarms by repository.alarmsFlow.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(if (initialNavigateTo != null) 1 else 0) }
    var activeNoteDetail by remember { mutableStateOf<Note?>(null) }
    var shouldAutoAddRow by remember { mutableStateOf(autoAddRow) }

    // Reakcja na przekazany intent
    LaunchedEffect(initialNavigateTo, initialTargetNoteId, notes) {
        if (initialNavigateTo == "NOTES_PRESSURE" && notes.isNotEmpty()) {
            val target = if (!initialTargetNoteId.isNullOrEmpty()) {
                notes.firstOrNull { it.id == initialTargetNoteId }
            } else {
                notes.firstOrNull { it.type == NoteType.PRESSURE_TABLE }
            }
            if (target != null) {
                selectedTab = 1
                activeNoteDetail = target
            }
        }
    }

    // Jeśli otwarto szczegóły notatki (np. tabelę ciśnień)
    if (activeNoteDetail != null) {
        val currentNote = notes.firstOrNull { it.id == activeNoteDetail?.id } ?: activeNoteDetail!!
        NoteDetailScreen(
            initialNote = currentNote,
            autoAddRowOnStart = shouldAutoAddRow,
            onBack = {
                activeNoteDetail = null
                shouldAutoAddRow = false
            },
            onSaveNote = { updated ->
                scope.launch {
                    repository.saveNote(updated)
                }
            }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Alarm, contentDescription = "Alarmy") },
                        label = { Text("Alarmy") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Notes, contentDescription = "Notes") },
                        label = { Text("Notes") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Statystyki") },
                        label = { Text("Statystyki") }
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (selectedTab) {
                    0 -> AlarmsScreen(
                        alarms = alarms,
                        notes = notes,
                        onToggleAlarm = { alarm, enabled ->
                            val updated = alarm.copy(isEnabled = enabled)
                            scope.launch {
                                repository.saveAlarm(updated)
                            }
                        },
                        onSaveAlarm = { alarm ->
                            scope.launch {
                                repository.saveAlarm(alarm)
                            }
                        },
                        onDeleteAlarm = { alarmId ->
                            scope.launch {
                                repository.deleteAlarm(alarmId)
                            }
                        },
                        onTestAlarm = onTestAlarm
                    )

                    1 -> NotesScreen(
                        notes = notes,
                        onOpenNote = { note ->
                            activeNoteDetail = note
                        },
                        onCreateNote = { newNote ->
                            scope.launch {
                                repository.saveNote(newNote)
                            }
                        },
                        onDeleteNote = { noteId ->
                            scope.launch {
                                repository.deleteNote(noteId)
                            }
                        }
                    )

                    2 -> StatsScreen(notes = notes)
                }
            }
        }
    }
}
