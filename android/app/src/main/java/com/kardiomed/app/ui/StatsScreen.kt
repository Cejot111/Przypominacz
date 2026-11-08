package com.kardiomed.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kardiomed.app.model.Note
import com.kardiomed.app.model.NoteType
import com.kardiomed.app.model.PressureEntry
import com.kardiomed.app.util.BpStatus
import com.kardiomed.app.util.PressurePatternEngine

@Composable
fun StatsScreen(notes: List<Note>) {
    val allEntries = remember(notes) {
        notes.filter { it.type == NoteType.PRESSURE_TABLE }
            .flatMap { it.pressureEntries }
            .filter { it.sys != null && it.dia != null }
    }

    val morningEntries = remember(allEntries) {
        allEntries.filter { it.slot.lowercase().contains("ran") }
    }

    val eveningEntries = remember(allEntries) {
        allEntries.filter { it.slot.lowercase().contains("wiecz") }
    }

    val avgMorningSys = morningEntries.mapNotNull { it.sys }.takeIf { it.isNotEmpty() }?.average()?.toInt()
    val avgMorningDia = morningEntries.mapNotNull { it.dia }.takeIf { it.isNotEmpty() }?.average()?.toInt()

    val avgEveningSys = eveningEntries.mapNotNull { it.sys }.takeIf { it.isNotEmpty() }?.average()?.toInt()
    val avgEveningDia = eveningEntries.mapNotNull { it.dia }.takeIf { it.isNotEmpty() }?.average()?.toInt()

    val totalValid = allEntries.size
    val optimalCount = allEntries.count {
        val s = PressurePatternEngine.classifyBp(it.sys, it.dia)
        s == BpStatus.OPTIMAL || s == BpStatus.NORMAL
    }
    val highCount = totalValid - optimalCount
    val optimalPercent = if (totalValid > 0) (optimalCount.toFloat() / totalValid.toFloat()) else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Analiza i Trendy Ciśnienia",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Porównanie pomiarów porannych i wieczornych",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // KARTY: RANO VS WIECZÓR
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Rano
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RANO", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFD97706))
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (avgMorningSys != null && avgMorningDia != null) {
                            Text(
                                text = "$avgMorningSys / $avgMorningDia",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val status = PressurePatternEngine.classifyBp(avgMorningSys, avgMorningDia)
                            Text(status.label, fontSize = 12.sp, color = Color(status.colorHex), fontWeight = FontWeight.SemiBold)
                        } else {
                            Text("Brak danych", fontSize = 15.sp, color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("${morningEntries.size} pomiarów", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                // Wieczór
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF6366F1).copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.NightsStay, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("WIECZÓR", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF4F46E5))
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (avgEveningSys != null && avgEveningDia != null) {
                            Text(
                                text = "$avgEveningSys / $avgEveningDia",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val status = PressurePatternEngine.classifyBp(avgEveningSys, avgEveningDia)
                            Text(status.label, fontSize = 12.sp, color = Color(status.colorHex), fontWeight = FontWeight.SemiBold)
                        } else {
                            Text("Brak danych", fontSize = 15.sp, color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("${eveningEntries.size} pomiarów", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        // ROZKŁAD W NORMIE
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Pomiary w normie kardiologicznej", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = optimalPercent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                        color = Color(0xFF10B981),
                        trackColor = Color(0xFFEF4444).copy(alpha = 0.3f)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🟢 W normie: $optimalCount (${(optimalPercent * 100).toInt()}%)",
                            fontSize = 12.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "🔴 Podwyższone: $highCount",
                            fontSize = 12.sp,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // TABELA NORM WHO
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Klasyfikacja norm ciśnienia (PTNT / ESH):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    NormRow(color = Color(0xFF10B981), name = "Optymalne", range = "< 120 / < 80 mmHg")
                    NormRow(color = Color(0xFF0D9488), name = "Prawidłowe", range = "120-129 / 80-84 mmHg")
                    NormRow(color = Color(0xFFF59E0B), name = "Wysokie prawidłowe", range = "130-139 / 85-89 mmHg")
                    NormRow(color = Color(0xFFF97316), name = "Nadciśnienie st. 1", range = "140-159 / 90-99 mmHg")
                    NormRow(color = Color(0xFFEF4444), name = "Nadciśnienie st. 2+", range = "≥ 160 / ≥ 100 mmHg")
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun NormRow(color: Color, name: String, range: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier.size(10.dp)
        ) {}
        Spacer(modifier = Modifier.width(8.dp))
        Text(name, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Text(range, fontSize = 12.sp, color = Color.Gray)
    }
}
