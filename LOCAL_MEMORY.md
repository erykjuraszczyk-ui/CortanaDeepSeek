# Local Memory — Cortana DeepSeek 1.0.0

- Historia rozmów: SQLite, maksymalnie 500 ostatnich wiadomości.
- Notatki pamięci: dodawane jawnie przez użytkownika, do 3000 znaków na notatkę.
- Notatki są zapisywane lokalnie i dołączane do kontekstu modelu jako dane, nie instrukcje.
- Notatkę można usunąć pojedynczo; osobna akcja usuwa wszystkie notatki.
- Czyszczenie historii czatu nie usuwa notatek.
- Migracja bazy danych z wersji 0.2.0 (schema 1) do 1.0.0 (schema 2) zachowuje historię wiadomości.

## Sprawdzenie ręczne
1. Uruchom lokalny serwer AI i wyślij wiadomość.
2. Zamknij i otwórz aplikację — historia powinna wrócić.
3. Dodaj notatkę w zakładce Pamięć.
4. Zadaj pytanie odnoszące się do notatki — powinna być przekazana w kontekście modelu.
5. Usuń notatkę i sprawdź, że znika z listy.
6. Wyczyść historię czatu — notatka powinna pozostać.
7. Wyczyść wszystkie notatki — model nie powinien otrzymać ich w kolejnych zapytaniach.
