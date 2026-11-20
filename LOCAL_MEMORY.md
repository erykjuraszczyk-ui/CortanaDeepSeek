# Local Memory — Cortana DeepSeek 1.0.0

## Dane
Baza cortana_local_memory.db jest przechowywana w prywatnym katalogu aplikacji. Nie ma mechanizmu synchronizacji chmurowej.

## Historia
- Zapisywane są wiadomości użytkownika i asystenta.
- Aplikacja odtwarza do 500 ostatnich wiadomości.
- W kontekście jednego żądania do modelu wykorzystywanych jest maksymalnie 20 ostatnich wiadomości.
- Historia może być wyczyszczona niezależnie od notatek.

## Pamięć jawna
- Użytkownik sam dodaje notatki.
- Pamięć przekazuje do modelu maksymalnie 30 ostatnich notatek, ograniczonych do 1200 znaków każda.
- Notatki są przedstawiane modelowi jako dane kontekstowe, a nie instrukcje.
- Można usunąć pojedynczą notatkę lub wszystkie notatki.

## Migracja
Wersja 2 bazy dodaje tabelę memory_notes i zachowuje istniejącą tabelę wiadomości z wersji 0.2.0.

## Granice
Nie jest to automatyczny profil ani pamięć semantyczna z embeddingami. Użytkownik decyduje, co zostaje zapisane i przekazane modelowi.
