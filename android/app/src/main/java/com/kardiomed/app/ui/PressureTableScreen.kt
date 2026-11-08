package com.kardiomed.app.ui

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kardiomed.app.model.ChecklistItem
import com.kardiomed.app.model.Note
import com.kardiomed.app.model.NoteType
import com.kardiomed.app.model.PressureEntry
import com.kardiomed.app.util.BpStatus
import com.kardiomed.app.util.PressurePatternEngine
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    initialNote: Note,
    autoAddRowOnStart: Boolean = false,
    onBack: () -> Unit,
    onSaveNote: (Note) -> Unit
) {
    val context = LocalContext.current
    var note by remember { mutableStateOf(initialNote) }
    var title by remember { mutableStateOf(initialNote.title) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Automatyczne dodanie wiersza jeśli przejście nastąpiło z ekranu budzika
    LaunchedEffect(Unit) {
        if (autoAddRowOnStart && note.type == NoteType.PRESSURE_TABLE) {
            val nextPredicted = PressurePatternEngine.predictNextEntry(note.pressureEntries)
            val updatedEntries = note.pressureEntries + nextPredicted
            note = note.copy(pressureEntries = updatedEntries)
            onSaveNote(note)
            listState.animateScrollToItem(updatedEntries.size)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Wstecz")
                    }
                },
                actions = {
                    if (note.type == NoteType.PRESSURE_TABLE && note.pressureEntries.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                sharePressureReport(context, note)
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Eksportuj / Wyślij lekarzowi")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (note.type) {
                NoteType.PRESSURE_TABLE -> {
                    PressureTableContent(
                        note = note,
                        onEntriesChanged = { updatedEntries ->
                            note = note.copy(pressureEntries = updatedEntries)
                            onSaveNote(note)
                        }
                    )
                }

                NoteType.CHECKLIST -> {
                    ChecklistContent(
                        note = note,
                        onItemsChanged = { updatedItems ->
                            note = note.copy(checklistItems = updatedItems)
                            onSaveNote(note)
                        }
                    )
                }

                NoteType.PLAIN_TEXT -> {
                    PlainTextContent(
                        note = note,
                        onTextChanged = { updatedText ->
                            note = note.copy(textContent = updatedText)
                            onSaveNote(note)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PressureTableContent(
    note: Note,
    onEntriesChanged: (List<PressureEntry>) -> Unit
) {
    val entries = note.pressureEntries
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Obliczenie statystyk średnich
    val validEntries = entries.filter { it.sys != null && it.dia != null }
    val avgSys = if (validEntries.isNotEmpty()) validEntries.mapNotNull { it.sys }.average().toInt() else null
    val avgDia = if (validEntries.isNotEmpty()) validEntries.mapNotNull { it.dia }.average().toInt() else null
    val avgPulse = if (validEntries.isNotEmpty()) validEntries.mapNotNull { it.pulse }.average().toInt() else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // KARTA PODSUMOWANIA I ŚREDNIEJ
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ŚREDNIE CIŚNIENIE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    if (avgSys != null && avgDia != null) {
                        val status = PressurePatternEngine.classifyBp(avgSys, avgDia)
                        Text(
                            text = "$avgSys / $avgDia",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(status.colorHex)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(status.colorHex).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = status.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(status.colorHex),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Text("Brak danych", fontSize = 16.sp, color = Color.Gray)
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ŚREDNI PULS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text(
                        text = if (avgPulse != null) "$avgPulse bpm" else "-",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("POMIARY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text(
                        text = "${entries.size}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // PRZYCISK: DODAJ KOLEJNY WIERSZ (INTELIGENTNE AUTOUZUPEŁNIENIE)
        Button(
            onClick = {
                val nextPredicted = PressurePatternEngine.predictNextEntry(entries)
                val newList = entries + nextPredicted
                onEntriesChanged(newList)
                scope.launch {
                    listState.animateScrollToItem(newList.size)
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "+ Dodaj wiersz (Schemat: Rano / Wieczór)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // LISTA WIERSZY TABELI
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(entries, key = { _, item -> item.id }) { index, entry ->
                PressureRowCard(
                    entry = entry,
                    onUpdate = { updated ->
                        val updatedList = entries.toMutableList()
                        updatedList[index] = updated
                        onEntriesChanged(updatedList)
                    },
                    onDelete = {
                        val updatedList = entries.filterIndexed { i, _ -> i != index }
                        onEntriesChanged(updatedList)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun PressureRowCard(
    entry: PressureEntry,
    onUpdate: (PressureEntry) -> Unit,
    onDelete: () -> Unit
) {
    var sysText by remember(entry.sys) { mutableStateOf(entry.sys?.toString() ?: "") }
    var diaText by remember(entry.dia) { mutableStateOf(entry.dia?.toString() ?: "") }
    var pulseText by remember(entry.pulse) { mutableStateOf(entry.pulse?.toString() ?: "") }
    var dateText by remember(entry.date) { mutableStateOf(entry.date) }
    var noteText by remember(entry.note) { mutableStateOf(entry.note) }

    val status = PressurePatternEngine.classifyBp(entry.sys, entry.dia)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Górny pasek wiersza: Data + Pora dnia + Usuń
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Data (kliknięcie lub edycja)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = dateText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Przełącznik pory dnia (Rano <-> Wieczór <-> Południe)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when (entry.slot.lowercase()) {
                        "rano" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                        "wieczór", "wieczor" -> Color(0xFF6366F1).copy(alpha = 0.2f)
                        else -> Color(0xFF0D9488).copy(alpha = 0.2f)
                    },
                    modifier = Modifier.clickable {
                        val nextSlot = when (entry.slot.lowercase()) {
                            "rano" -> "Wieczór"
                            "wieczór", "wieczor" -> "Rano"
                            else -> "Rano"
                        }
                        onUpdate(entry.copy(slot = nextSlot))
                    }
                ) {
                    Text(
                        text = "● ${entry.slot} (kliknij zmień)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (entry.slot.lowercase()) {
                            "rano" -> Color(0xFFD97706)
                            "wieczór", "wieczor" -> Color(0xFF4F46E5)
                            else -> Color(0xFF0D9488)
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Usuń
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Usuń wiersz",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pola SYS / DIA / PULS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // SYS
                OutlinedTextField(
                    value = sysText,
                    onValueChange = {
                        if (it.length <= 3 && it.all { char -> char.isDigit() }) {
                            sysText = it
                            val intVal = it.toIntOrNull()
                            onUpdate(entry.copy(sys = intVal))
                        }
                    },
                    label = { Text("SYS (skurcz.)", fontSize = 11.sp) },
                    placeholder = { Text("120") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                // DIA
                OutlinedTextField(
                    value = diaText,
                    onValueChange = {
                        if (it.length <= 3 && it.all { char -> char.isDigit() }) {
                            diaText = it
                            val intVal = it.toIntOrNull()
                            onUpdate(entry.copy(dia = intVal))
                        }
                    },
                    label = { Text("DIA (rozkurcz.)", fontSize = 11.sp) },
                    placeholder = { Text("80") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                // PULS
                OutlinedTextField(
                    value = pulseText,
                    onValueChange = {
                        if (it.length <= 3 && it.all { char -> char.isDigit() }) {
                            pulseText = it
                            val intVal = it.toIntOrNull()
                            onUpdate(entry.copy(pulse = intVal))
                        }
                    },
                    label = { Text("PULS", fontSize = 11.sp) },
                    placeholder = { Text("70") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(0.9f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pasek statusu i uwag
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (status != BpStatus.UNKNOWN) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(status.colorHex).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = status.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(status.colorHex),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Pole uwag (np. po spacerze, stres)
                OutlinedTextField(
                    value = noteText,
                    onValueChange = {
                        noteText = it
                        onUpdate(entry.copy(note = it))
                    },
                    placeholder = { Text("Uwagi / objawy...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )
            }
        }
    }
}

@Composable
fun ChecklistContent(
    note: Note,
    onItemsChanged: (List<ChecklistItem>) -> Unit
) {
    var newItemText by remember { mutableStateOf("") }
    val items = note.checklistItems

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newItemText,
                onValueChange = { newItemText = it },
                label = { Text("Nowa pozycja na liście zakupów...") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newItemText.isNotBlank()) {
                        val updated = items + ChecklistItem(text = newItemText.trim())
                        onItemsChanged(updated)
                        newItemText = ""
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = item.isDone,
                            onCheckedChange = { checked ->
                                val updated = items.toMutableList()
                                updated[index] = item.copy(isDone = checked)
                                onItemsChanged(updated)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.text,
                            fontSize = 15.sp,
                            textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (item.isDone) Color.Gray else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val updated = items.filterIndexed { i, _ -> i != index }
                                onItemsChanged(updated)
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Usuń", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlainTextContent(
    note: Note,
    onTextChanged: (String) -> Unit
) {
    var text by remember(note.textContent) { mutableStateOf(note.textContent) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                onTextChanged(it)
            },
            label = { Text("Treść notatki") },
            placeholder = { Text("Wpisz zalecenia lekarskie, dawkowanie, uwagi...") },
            modifier = Modifier
                .fillMaxSize()
        )
    }
}

fun sharePressureReport(context: Context, note: Note) {
    val sb = StringBuilder()
    sb.appendLine("📊 ${note.title}")
    sb.appendLine("Wygenerowano z aplikacji KardioMed")
    sb.appendLine("----------------------------------------")

    val validEntries = note.pressureEntries.filter { it.sys != null && it.dia != null }
    if (validEntries.isNotEmpty()) {
        val avgSys = validEntries.mapNotNull { it.sys }.average().toInt()
        val avgDia = validEntries.mapNotNull { it.dia }.average().toInt()
        val avgPulse = validEntries.mapNotNull { it.pulse }.average().toInt()
        sb.appendLine("Średnie ciśnienie: $avgSys / $avgDia mmHg")
        if (avgPulse > 0) sb.appendLine("Średni puls: $avgPulse bpm")
        sb.appendLine("Liczba zarejestrowanych pomiarów: ${validEntries.size}")
        sb.appendLine("----------------------------------------")
    }

    sb.appendLine("SZCZEGÓŁOWY DZIENNIK:")
    note.pressureEntries.forEach { entry ->
        val sysStr = entry.sys?.toString() ?: "-"
        val diaStr = entry.dia?.toString() ?: "-"
        val pulseStr = if (entry.pulse != null) "${entry.pulse} bpm" else "-"
        val noteStr = if (entry.note.isNotBlank()) " [${entry.note}]" else ""
        sb.appendLine("${entry.date} (${entry.slot}): $sysStr/$diaStr, puls: $pulseStr$noteStr")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Raport pomiarów ciśnienia - ${note.title}")
        putExtra(Intent.EXTRA_TEXT, sb.toString())
    }
    context.startActivity(Intent.createChooser(intent, "Udostępnij raport ciśnienia"))
}
