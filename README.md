# 🩺 KardioMed — Notatnik & Głośny Budzik Medyczny na Androida

Nowoczesna aplikacja na system Android napisana w **Kotlin + Jetpack Compose (Material 3)**.

---

## ✨ Kluczowe Funkcje

### ⏰ 1. Prawdziwe, Głośne Alarmy Medyczne (nie tylko ciche powiadomienia)
* **Budzik medyczny:** Wykorzystuje `AlarmManager.setAlarmClock()`, dzięki czemu dzwoni nawet przy zablokowanym i wyciszonym telefonie oraz w trybie oszczędzania baterii.
* **Wybór dźwięków:**
  * Klasyczny energiczny budzik
  * Sygnał medyczny (pulsometr)
  * Łagodny gong
  * Cyfrowy alarm
  * Własny dźwięk z pamięci telefonu (systemowy Ringtone Picker)
* **Ekran budzika:** Pełnoekranowy widok z opcją Wyłącz, Drzemka (+10 min) oraz **„Zapisz pomiar w tabeli”**.
* **Typy alarmów:** Cykliczne (wybrane dni tygodnia) oraz jednorazowe.

### 📝 2. Inteligentny Notes & Tabela Pomiarów
* **Schemat i autouzupełnianie:**
  * Wpisujesz: `2026-11-08 | Rano`
  * Wpisujesz w kolejnym: `2026-11-08 | Wieczór`
  * Kliknięcie `+ Dodaj wiersz` automatycznie tworzy: **`2026-11-09 | Rano`**!
  * Kolejny wiersz: **`2026-11-09 | Wieczór`**.
  * Pełne wsparcie dla schematów 2-fazowych (Rano / Wieczór), 3-fazowych (Rano / Południe / Wieczór) lub 1-fazowych.
* **Klasyfikacja ciśnienia:** Automatyczne oznaczanie norm PTNT/ESH kolorami (Optymalne, Prawidłowe, Nadciśnienie st. 1, st. 2, Kryzys).
* **Lista zakupów:** Checkboxy z odznaczaniem zakupionych leków w aptece.
* **Czysty tekst:** Notatki na zalecenia lekarza.

### 📊 3. Statystyki & Eksport
* Średnie ciśnienie poranne vs wieczorne.
* Procent pomiarów w normie.
* Eksport jednym kliknięciem do wiadomości (SMS / WhatsApp / Email dla lekarza).

---

## 🚀 Jak pobrać aplikację (APK)

Po wysłaniu kodu na GitHub (`wyslij_na_github.bat`), usługa **GitHub Actions** automatycznie kompiluje gotowy plik `.apk`:
1. Przejdź do zakładki **Releases** na swoim GitHubie: `https://github.com/Cejot111/<repo_name>/releases`
2. Pobierz plik **`KardioMed-v1.0.0.apk`** bezpośrednio na telefon i zainstaluj!
