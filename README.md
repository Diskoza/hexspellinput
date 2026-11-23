# Hex Spell Input

Client-side addon for Minecraft Forge 1.20.1 and Hex Casting 0.11.3. It adds a text field to the standard Hex Casting staff screen and draws the entered patterns using the same internal actions as manual drawing. The addon has no items, network channels or server-side part of its own.

## Installation

1. Install Forge 47.x, Hex Casting **0.11.3** and its usual dependencies.
2. Put the Hex Spell Input JAR into the client `mods` folder.
3. You do not need to install Hex Spell Input on the server.

Some servers may forbid automated input. Check the server rules before using it.

## Input

- `aqaa`: a single pattern
- `aqaa qqq`: several patterns
- `sw:aqaa`: a pattern with the south-west starting direction
- `-`: an empty signature (a single straight line)

Valid directions: `ne:`, `e:`, `se:`, `sw:`, `w:`, `nw:`. Patterns can be separated by spaces, pasted line breaks, commas, semicolons or `|`. Case does not matter. Press Enter to start drawing.

The whole queue is validated first. After each pattern the addon waits for the regular Hex Casting response before placing the next one. The "Stop" button cancels the queue without removing patterns that were already accepted.

## Building and running

```powershell
.\gradlew.bat clean build
.\gradlew.bat runClient
```

Hex Casting is used as an external Gradle dependency and is not bundled into the addon JAR.
