package com.kardiomed.app.util

import com.kardiomed.app.model.PressureEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class BpStatus(val label: String, val colorHex: Long) {
    OPTIMAL("Optymalne", 0xFF10B981),        // Zielony
    NORMAL("Prawidłowe", 0xFF0D9488),        // Morski
    HIGH_NORMAL("Wysokie praw.", 0xFFF59E0B),// Bursztynowy
    GRADE1("Nadciśnienie 1°", 0xFFF97316),   // Pomarańczowy
    GRADE2("Nadciśnienie 2°", 0xFFEF4444),   // Czerwony
    CRISIS("Kryzys!", 0xFFB91C1C),           // Ciemnoczerwony
    UNKNOWN("Brak danych", 0xFF64748B)       // Szary
}

object PressurePatternEngine {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun classifyBp(sys: Int?, dia: Int?): BpStatus {
        if (sys == null || dia == null) return BpStatus.UNKNOWN
        return when {
            sys >= 180 || dia >= 120 -> BpStatus.CRISIS
            sys >= 160 || dia >= 100 -> BpStatus.GRADE2
            sys >= 140 || dia >= 90  -> BpStatus.GRADE1
            sys >= 130 || dia >= 85  -> BpStatus.HIGH_NORMAL
            sys >= 120 || dia >= 80  -> BpStatus.NORMAL
            else -> BpStatus.OPTIMAL
        }
    }

    /**
     * Inteligentne przewidywanie kolejnego wiersza na podstawie schematu wpisów użytkownika.
     */
    fun predictNextEntry(existingEntries: List<PressureEntry>): PressureEntry {
        val now = Calendar.getInstance()
        val todayStr = dateFormat.format(now.time)
        val currentHour = now.get(Calendar.HOUR_OF_DAY)

        if (existingEntries.isEmpty()) {
            val initialSlot = if (currentHour < 14) "Rano" else "Wieczór"
            return PressureEntry(date = todayStr, slot = initialSlot)
        }

        val last = existingEntries.last()
        val lastSlotClean = last.slot.trim().lowercase(Locale.getDefault())

        // Odczytaj datę z ostatniego wpisu lub użyj dzisiejszej
        val lastCal = Calendar.getInstance().apply {
            time = runCatching { dateFormat.parse(last.date) }.getOrNull() ?: Date()
        }

        // Zbadaj historię ostatnich do 6 wpisów, by wykryć schemat
        val recentSlots = existingEntries.takeLast(6).map {
            it.slot.trim().lowercase(Locale.getDefault())
        }

        val hasMorning = recentSlots.any { it.contains("ran") }
        val hasEvening = recentSlots.any { it.contains("wiecz") }
        val hasNoon = recentSlots.any { it.contains("poł") || it.contains("obiad") }

        // SCHEMAT 3-FAZOWY: Rano -> Południe -> Wieczór
        if (hasMorning && hasNoon && hasEvening) {
            return when {
                lastSlotClean.contains("ran") -> {
                    PressureEntry(date = last.date, slot = "Południe")
                }
                lastSlotClean.contains("poł") || lastSlotClean.contains("obiad") -> {
                    PressureEntry(date = last.date, slot = "Wieczór")
                }
                else -> {
                    // Po wieczorze kolejny dzień i Rano
                    lastCal.add(Calendar.DAY_OF_YEAR, 1)
                    val nextDateStr = dateFormat.format(lastCal.time)
                    PressureEntry(date = nextDateStr, slot = "Rano")
                }
            }
        }

        // SCHEMAT 2-FAZOWY: Rano <-> Wieczór (najczęstszy zalecany przez kardiologów)
        if (hasMorning && hasEvening) {
            return if (lastSlotClean.contains("ran")) {
                // Po porannym pomiarze -> ten sam dzień, pora Wieczór
                PressureEntry(date = last.date, slot = "Wieczór")
            } else {
                // Po wieczornym pomiarze -> następny dzień, pora Rano
                lastCal.add(Calendar.DAY_OF_YEAR, 1)
                val nextDateStr = dateFormat.format(lastCal.time)
                PressureEntry(date = nextDateStr, slot = "Rano")
            }
        }

        // SCHEMAT 1-FAZOWY: Tylko Rano
        if (hasMorning && !hasEvening) {
            lastCal.add(Calendar.DAY_OF_YEAR, 1)
            val nextDateStr = dateFormat.format(lastCal.time)
            return PressureEntry(date = nextDateStr, slot = "Rano")
        }

        // SCHEMAT 1-FAZOWY: Tylko Wieczór
        if (hasEvening && !hasMorning) {
            lastCal.add(Calendar.DAY_OF_YEAR, 1)
            val nextDateStr = dateFormat.format(lastCal.time)
            return PressureEntry(date = nextDateStr, slot = "Wieczór")
        }

        // DOMYŚLNY FALLBACK
        return if (lastSlotClean.contains("ran")) {
            PressureEntry(date = last.date, slot = "Wieczór")
        } else {
            lastCal.add(Calendar.DAY_OF_YEAR, 1)
            val nextDateStr = dateFormat.format(lastCal.time)
            PressureEntry(date = nextDateStr, slot = "Rano")
        }
    }
}
