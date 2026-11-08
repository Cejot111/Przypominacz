package com.kardiomed.app.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kardiomed.app.alarm.AlarmScheduler
import com.kardiomed.app.alarm.AlarmSoundPlayer
import com.kardiomed.app.data.AppRepository
import com.kardiomed.app.model.AlarmItem
import com.kardiomed.app.model.AlarmType
import com.kardiomed.app.model.Note
import com.kardiomed.app.model.NoteType
import com.kardiomed.app.model.SoundPresets
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun AlarmsScreen(
    alarms: List<AlarmItem>,
    notes: List<Note>,
    onToggleAlarm: (AlarmItem, Boolean) -> Unit,
    onSaveAlarm: (AlarmItem) -> Unit,
    onDeleteAlarm: (String) -> Unit,
    onTestAlarm: (AlarmItem) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<AlarmItem?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingAlarm = null
                    showDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj alarm")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Nagłówek i info o głośnym budziku
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Głośne alarmy medyczne",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Dzwonią z pełną głośnością budzika nawet przy wyciszonym telefonie.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (alarms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Brak aktywnych przypomnień",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Kliknij przycisk +, aby dodać poranny lub wieczorny pomiar ciśnienia lub leki.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmCard(
                            alarm = alarm,
                            onToggle = { enabled -> onToggleAlarm(alarm, enabled) },
                            onEdit = {
                                editingAlarm = alarm
                                showDialog = true
                            },
                            onDelete = { onDeleteAlarm(alarm.id) },
                            onTest = { onTestAlarm(alarm) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlarmEditDialog(
            alarmToEdit = editingAlarm,
            availableNotes = notes.filter { it.type == NoteType.PRESSURE_TABLE },
            onDismiss = { showDialog = false },
            onSave = { savedAlarm ->
                onSaveAlarm(savedAlarm)
                showDialog = false
            }
        )
    }
}

@Composable
fun AlarmCard(
    alarm: AlarmItem,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    val isPressure = alarm.type == AlarmType.PRESSURE
    val isMeds = alarm.type == AlarmType.MEDICATION
    val typeColor = if (isPressure) Color(0xFFEF4444) else if (isMeds) Color(0xFF0D9488) else Color(0xFF6366F1)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (alarm.isEnabled) 3.dp else 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ikona typu
                Surface(
                    shape = CircleShape,
                    color = typeColor.copy(alpha = if (alarm.isEnabled) 0.15f else 0.08f),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPressure) Icons.Default.Favorite else Icons.Default.Notifications,
                            contentDescription = null,
                            tint = if (alarm.isEnabled) typeColor else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Godzina i tytuł
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alarm.timeFormatted(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurface else Color.Gray
                    )
                    Text(
                        text = alarm.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurface else Color.Gray
                    )
                }

                // Przełącznik Wł / Wył
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Etykiety i dni
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = alarm.daysFormatted(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Nazwa dźwięku
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = SoundPresets.getDisplayName(alarm.soundPreset),
                            fontSize = 11.sp,
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }
                }

                // Przyciski akcji
                Row {
                    IconButton(onClick = onTest, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Testuj dźwięk",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edytuj",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Usuń",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlarmEditDialog(
    alarmToEdit: AlarmItem?,
    availableNotes: List<Note>,
    onDismiss: () -> Unit,
    onSave: (AlarmItem) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(alarmToEdit?.title ?: "Poranny pomiar ciśnienia") }
    var type by remember { mutableStateOf(alarmToEdit?.type ?: AlarmType.PRESSURE) }
    var hour by remember { mutableIntStateOf(alarmToEdit?.hour ?: 8) }
    var minute by remember { mutableIntStateOf(alarmToEdit?.minute ?: 0) }
    var isRecurring by remember { mutableStateOf(alarmToEdit?.isRecurring ?: true) }
    var daysOfWeek by remember { mutableStateOf(alarmToEdit?.daysOfWeek ?: (1..7).toSet()) }
    var soundPreset by remember { mutableStateOf(alarmToEdit?.soundPreset ?: SoundPresets.CLASSIC_ALARM) }
    var customUriStr by remember { mutableStateOf(alarmToEdit?.customSoundUri) }
    var vibrate by remember { mutableStateOf(alarmToEdit?.vibrate ?: true) }
    var targetNoteId by remember { mutableStateOf(alarmToEdit?.targetNoteId ?: availableNotes.firstOrNull()?.id) }

    var isPreviewPlaying by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            AlarmSoundPlayer.stop()
        }
    }

    // Selektor systemowego dzwonka
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                customUriStr = uri.toString()
                soundPreset = SoundPresets.CUSTOM_SYSTEM
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            AlarmSoundPlayer.stop()
            onDismiss()
        },
        title = {
            Text(
                text = if (alarmToEdit == null) "Nowe przypomnienie" else "Edycja przypomnienia",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Tytuł
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tytuł alarmu") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Typ przypomnienia
                Text("Typ powiadomienia:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AlarmType.values().forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = {
                                type = t
                                if (title.isEmpty() || title == "Poranny pomiar ciśnienia" || title == "Zażycie leków") {
                                    title = t.labelPl
                                }
                            },
                            label = { Text(t.labelPl, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Wybór godziny i minuty
                Text("Godzina alarmu:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(String.format("%02d", hour), fontSize = 36.sp, fontWeight = FontWeight.Bold)
                        Row {
                            OutlinedButton(onClick = { hour = (hour - 1 + 24) % 24 }) { Text("-") }
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedButton(onClick = { hour = (hour + 1) % 24 }) { Text("+") }
                        }
                    }

                    Text(":", fontSize = 36.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(String.format("%02d", minute), fontSize = 36.sp, fontWeight = FontWeight.Bold)
                        Row {
                            OutlinedButton(onClick = { minute = (minute - 5 + 60) % 60 }) { Text("-5") }
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedButton(onClick = { minute = (minute + 5) % 60 }) { Text("+5") }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cykliczność
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Powtarzaj cyklicznie", fontWeight = FontWeight.Medium)
                    Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
                }

                if (isRecurring) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Dni tygodnia:", fontSize = 12.sp, color = Color.Gray)
                    val daysMap = listOf(1 to "Pn", 2 to "Wt", 3 to "Śr", 4 to "Cz", 5 to "Pt", 6 to "Sb", 7 to "Nd")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        daysMap.forEach { (dNum, dName) ->
                            val selected = daysOfWeek.contains(dNum)
                            Surface(
                                shape = CircleShape,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable {
                                        daysOfWeek = if (selected) daysOfWeek - dNum else daysOfWeek + dNum
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = dName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // WYBÓR DŹWIĘKU ALARMU
                Text("Dźwięk alarmu:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                SoundPresets.ALL.forEach { (presetKey, displayName) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                soundPreset = presetKey
                                if (presetKey == SoundPresets.CUSTOM_SYSTEM) {
                                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_RINGTONE)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                    }
                                    ringtonePickerLauncher.launch(intent)
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (soundPreset == presetKey),
                            onClick = {
                                soundPreset = presetKey
                                if (presetKey == SoundPresets.CUSTOM_SYSTEM) {
                                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_RINGTONE)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                    }
                                    ringtonePickerLauncher.launch(intent)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = displayName,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Przycisk odsłuchu wybranego dźwięku
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        if (isPreviewPlaying) {
                            AlarmSoundPlayer.stop()
                            isPreviewPlaying = false
                        } else {
                            isPreviewPlaying = true
                            AlarmSoundPlayer.previewSound(context, soundPreset, customUriStr) {
                                isPreviewPlaying = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isPreviewPlaying) "Zatrzymaj odsłuch" else "Przetestuj wybrany dźwięk")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Wibracja
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Wibracje przy alarmie")
                    }
                    Switch(checked = vibrate, onCheckedChange = { vibrate = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    AlarmSoundPlayer.stop()
                    val newAlarm = AlarmItem(
                        id = alarmToEdit?.id ?: UUID.randomUUID().toString(),
                        title = title.ifBlank { type.labelPl },
                        type = type,
                        hour = hour,
                        minute = minute,
                        daysOfWeek = if (isRecurring) daysOfWeek else emptySet(),
                        isEnabled = true,
                        isRecurring = isRecurring,
                        soundPreset = soundPreset,
                        customSoundUri = customUriStr,
                        vibrate = vibrate,
                        targetNoteId = targetNoteId
                    )
                    onSave(newAlarm)
                }
            ) {
                Text("Zapisz alarm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    AlarmSoundPlayer.stop()
                    onDismiss()
                }
            ) {
                Text("Anuluj")
            }
        }
    )
}
