package com.kardiomed.app.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kardiomed.app.model.ChecklistItem
import com.kardiomed.app.model.Note
import com.kardiomed.app.model.NoteType
import com.kardiomed.app.util.PressurePatternEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun NotesScreen(
    notes: List<Note>,
    onOpenNote: (Note) -> Unit,
    onCreateNote: (Note) -> Unit,
    onDeleteNote: (String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf<NoteType?>(null) }
    var showCreateMenu by remember { mutableStateOf(false) }
    var createTypeDialog by remember { mutableStateOf<NoteType?>(null) }
    var newNoteTitle by remember { mutableStateOf("") }

    val filteredNotes = remember(notes, selectedFilter) {
        if (selectedFilter == null) notes else notes.filter { it.type == selectedFilter }
    }

    Scaffold(
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { showCreateMenu = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Dodaj notatkę")
                }

                DropdownMenu(
                    expanded = showCreateMenu,
                    onDismissRequest = { showCreateMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("🛒 Lista do sklepu (Spożywcze)") },
                        leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981)) },
                        onClick = {
                            showCreateMenu = false
                            newNoteTitle = "Lista do sklepu"
                            createTypeDialog = NoteType.CHECKLIST
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🔑 Przed wyjściem z domu (ADHD)") },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B)) },
                        onClick = {
                            showCreateMenu = false
                            newNoteTitle = "Przed wyjściem z domu"
                            createTypeDialog = NoteType.CHECKLIST
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("📊 Tabela pomiarów (ze schematem)") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFFEF4444)) },
                        onClick = {
                            showCreateMenu = false
                            newNoteTitle = "Pomiary ciśnienia"
                            createTypeDialog = NoteType.PRESSURE_TABLE
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🧠 Zrzut myśli (Brain Dump ADHD)") },
                        leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFF8B5CF6)) },
                        onClick = {
                            showCreateMenu = false
                            newNoteTitle = "Zrzut myśli (Brain Dump)"
                            createTypeDialog = NoteType.PLAIN_TEXT
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("📝 Czysta notatka tekstowa") },
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF6366F1)) },
                        onClick = {
                            showCreateMenu = false
                            newNoteTitle = "Nowa notatka"
                            createTypeDialog = NoteType.PLAIN_TEXT
                        }
                    )
                }
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

            // Filtry notatek
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("Wszystkie", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = selectedFilter == NoteType.PRESSURE_TABLE,
                    onClick = { selectedFilter = NoteType.PRESSURE_TABLE },
                    label = { Text("Pomiary", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = selectedFilter == NoteType.CHECKLIST,
                    onClick = { selectedFilter = NoteType.CHECKLIST },
                    label = { Text("Listy zakupów", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = selectedFilter == NoteType.PLAIN_TEXT,
                    onClick = { selectedFilter = NoteType.PLAIN_TEXT },
                    label = { Text("Tekst", fontSize = 12.sp) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Brak notatek w tej kategorii",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Dotknij przycisku +, aby utworzyć tabelę pomiarów ciśnienia lub listę zakupów.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { onOpenNote(note) },
                            onDelete = { onDeleteNote(note.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Dialog tworzenia nowej notatki
    createTypeDialog?.let { targetType ->
        AlertDialog(
            onDismissRequest = { createTypeDialog = null },
            title = {
                Text(
                    text = when (targetType) {
                        NoteType.PRESSURE_TABLE -> "Nowa tabela pomiarów ciśnienia"
                        NoteType.CHECKLIST -> "Nowa lista zakupów / zadań"
                        NoteType.PLAIN_TEXT -> "Nowa notatka tekstowa"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = newNoteTitle,
                        onValueChange = { newNoteTitle = it },
                        label = { Text("Tytuł notesu") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (targetType) {
                            NoteType.PRESSURE_TABLE -> "Tabela z automatycznym schematem uzupełniania dat i pór (Rano / Wieczór)."
                            NoteType.CHECKLIST -> "Wygodna lista z zaznaczanymi polami wyboru."
                            NoteType.PLAIN_TEXT -> "Czyste pole na notatki, zalecenia lekarskie i uwagi."
                        },
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val initialItems = when {
                            targetType == NoteType.CHECKLIST && newNoteTitle.contains("sklep", ignoreCase = true) -> listOf(
                                ChecklistItem(text = "🛒 Chleb / pieczywo"),
                                ChecklistItem(text = "🥛 Mleko"),
                                ChecklistItem(text = "🥚 Jajka"),
                                ChecklistItem(text = "☕ Kawa"),
                                ChecklistItem(text = "🍎 Owoce")
                            )
                            targetType == NoteType.CHECKLIST && newNoteTitle.contains("wyjściem", ignoreCase = true) -> listOf(
                                ChecklistItem(text = "🔑 Klucze od domu"),
                                ChecklistItem(text = "💳 Portfel / dokumenty"),
                                ChecklistItem(text = "📱 Telefon naładowany"),
                                ChecklistItem(text = "💧 Butelka z wodą"),
                                ChecklistItem(text = "🔌 Wyłączona kuchenka i żelazko")
                            )
                            else -> emptyList()
                        }
                        val newNote = Note(
                            id = UUID.randomUUID().toString(),
                            title = newNoteTitle.ifBlank { "Nowa notatka" },
                            type = targetType,
                            checklistItems = initialItems
                        )
                        onCreateNote(newNote)
                        createTypeDialog = null
                        onOpenNote(newNote)
                    }
                ) {
                    Text("Utwórz")
                }
            },
            dismissButton = {
                TextButton(onClick = { createTypeDialog = null }) {
                    Text("Anuluj")
                }
            }
        )
    }
}

@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val df = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale("pl")) }
    val formattedDate = remember(note.updatedAt) { df.format(Date(note.updatedAt)) }

    val icon = when (note.type) {
        NoteType.PRESSURE_TABLE -> Icons.Default.Favorite
        NoteType.CHECKLIST -> Icons.Default.Check
        NoteType.PLAIN_TEXT -> Icons.Default.Description
    }

    val typeColor = when (note.type) {
        NoteType.PRESSURE_TABLE -> Color(0xFFEF4444)
        NoteType.CHECKLIST -> Color(0xFF0D9488)
        NoteType.PLAIN_TEXT -> Color(0xFF6366F1)
    }

    val typeLabel = when (note.type) {
        NoteType.PRESSURE_TABLE -> "Tabela ciśnień"
        NoteType.CHECKLIST -> "Lista zadań/zakupów"
        NoteType.PLAIN_TEXT -> "Notatka"
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = typeColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = typeColor, modifier = Modifier.size(22.dp))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = note.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = typeLabel,
                        fontSize = 12.sp,
                        color = typeColor,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Usuń",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Podgląd zawartości
            when (note.type) {
                NoteType.PRESSURE_TABLE -> {
                    val count = note.pressureEntries.size
                    val lastEntry = note.pressureEntries.lastOrNull()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Liczba pomiarów: $count",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (lastEntry?.sys != null && lastEntry.dia != null) {
                            val status = PressurePatternEngine.classifyBp(lastEntry.sys, lastEntry.dia)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(status.colorHex).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Ostatni: ${lastEntry.sys}/${lastEntry.dia} (${lastEntry.slot})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(status.colorHex),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                NoteType.CHECKLIST -> {
                    val total = note.checklistItems.size
                    val done = note.checklistItems.count { it.isDone }
                    val percent = if (total > 0) (done * 100 / total) else 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kupiono / Zrobiono: $done z $total",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (done == total && total > 0) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF6366F1).copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "$percent%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (done == total && total > 0) Color(0xFF10B981) else Color(0xFF6366F1),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                NoteType.PLAIN_TEXT -> {
                    Text(
                        text = note.textContent.ifBlank { "(Pusta notatka)" },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Zaktualizowano: $formattedDate",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}
