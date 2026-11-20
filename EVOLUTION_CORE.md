# Evolution Core — Cortana DeepSeek 1.0.0

## Cel
Rozwijać aplikację bez nieograniczonego dostępu modelu do urządzenia, repozytorium ani procesu wydawniczego.

## Cykl
1. Obserwacja: jawne logi błędów i zgłoszenia.
2. Analiza: opis problemu, wpływu i ryzyka.
3. Propozycja: patch w oddzielnej gałęzi; stabilne wydanie pozostaje nietknięte.
4. Weryfikacja: build, testy, sprawdzenie uprawnień i diffu.
5. Akceptacja: właściciel projektu zatwierdza zmianę.
6. Wydanie: CI tworzy artefakty; klucz release jest przechowywany poza repozytorium.

## Ograniczenia bezpieczeństwa
- Model nie wykonuje wygenerowanego kodu.
- Model nie uruchamia powłoki systemowej.
- Model nie instaluje APK ani nie przyznaje sobie uprawnień.
- Model nie czyta danych spoza jawnie udostępnionych źródeł.
- Klucz podpisujący nie trafia do repozytorium ani promptu.

Jakość poprawiamy przez testy regresji, wersjonowanie, przegląd zmian i możliwość wycofania wydania.
