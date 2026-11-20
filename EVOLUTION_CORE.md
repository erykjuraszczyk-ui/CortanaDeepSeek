# Evolution Core — Cortana DeepSeek 1.0.0

## Cel
Rozwój aplikacji z kontrolą zmian, bezpieczeństwa i jakości.

## Cykl
1. Obserwacja: jawne logi błędów i zgłoszenia.
2. Analiza: opis problemu, wpływu i ryzyka.
3. Propozycja: patch w oddzielnej gałęzi.
4. Weryfikacja: kompilacja, testy, przegląd uprawnień i diffu.
5. Akceptacja: właściciel projektu zatwierdza zmianę.
6. Wydanie: CI buduje APK; podpis release odbywa się kluczem przechowywanym poza repozytorium.

## Granice uprawnień
- Model nie wykonuje kodu wygenerowanego przez AI.
- Model nie uruchamia powłoki systemowej.
- Model nie instaluje APK ani nie przyznaje sobie uprawnień.
- Klucz podpisujący nie trafia do repozytorium ani promptu.
- Aplikacja nie próbuje uzyskiwać uprawnień administratora/root.

## Kryteria jakości
Każda zmiana powinna mieć testy, kryteria akceptacji, wersję i możliwość wycofania.
