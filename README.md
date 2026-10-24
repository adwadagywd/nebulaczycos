# Nebula (Fabric 1.21.4)

Moduły: ESP, Tracers, Nametags, Fullbright, HitboxExpander, Freecam, TriggerBot.

## Build
Wymagania: JDK 21.
    gradle wrapper --gradle-version 8.12   # jednorazowo (albo po prostu otwórz projekt w IntelliJ)
    ./gradlew build
Gotowy jar: build/libs/nebula-1.0.0.jar -> wrzuć do .minecraft/mods razem z Fabric API.
Dev: ./gradlew runClient

## Obsługa
- RIGHT SHIFT otwiera menu (bind można zmienić w menu).
- Menu: LPM na module = wybór, PPM = włącz/wyłącz. Po prawej ustawienia; "Bind" -> kliknij i naciśnij klawisz
  (BACKSPACE/DELETE lub PPM = usuń bind, ESC = anuluj).
- Każde włączenie/wyłączenie pokazuje powiadomienie w prawym górnym rogu.
- Konfiguracja (ustawienia + bindy) zapisuje się w config/nebula.json.
