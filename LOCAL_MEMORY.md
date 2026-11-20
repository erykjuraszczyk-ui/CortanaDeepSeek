# Local Memory — Cortana DeepSeek 0.2.0

## Zakres
Wersja 0.2.0 dodaje trwałe archiwum rozmów oparte na Android SQLiteOpenHelper. Baza `cortana_local_memory.db` znajduje się w prywatnym katalogu aplikacji.

## Zachowanie
- Wiadomości użytkownika i asystenta są zapisywane lokalnie.
- Po uruchomieniu aplikacja odtwarza maksymalnie 500 ostatnich wiadomości.
- Do kontekstu modelu trafia maksymalnie 20 ostatnich elementów rozmowy w bieżącym działaniu aplikacji.
- Użytkownik może wyczyścić historię po potwierdzeniu.
- Dane nie są synchronizowane z usługą zewnętrzną.

## Granice
To jest historia czatu, nie pamięć semantyczna. Nie wyciąga automatycznie faktów, nie tworzy profilu użytkownika i nie przeszukuje archiwum embeddingami. Kolejny etap powinien dodać jawne, kontrolowane przez użytkownika notatki pamięci, wyszukiwanie oraz eksport/usuwanie danych.

## Test ręczny
1. Zbuduj i zainstaluj APK 0.2.0.
2. Wyślij wiadomość przy działającym lokalnym serwerze.
3. Zamknij aplikację i uruchom ją ponownie — wiadomości powinny wrócić.
4. Użyj „Wyczyść pamięć” i potwierdź.
5. Zamknij i uruchom aplikację ponownie — powinna być tylko nowa wiadomość powitalna.
