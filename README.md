
# Hypixel Skyblock Timer

A desktop timer for Hypixel Skyblock forges, Kat pet upgrades, and any custom
grind. Tracks live Bazaar margins and records profit each time a forge timer
completes.

## Requirements

- - **Java 25** — [Download Eclipse Temurin 25](https://adoptium.net/temurin/releases/?version=25)
- A free [Hypixel developer API key](https://developer.hypixel.net/)
- Download updated releases on right
## Setup

1. Open `secrets.properties`
2. Paste your Hypixel API key into it
3. Put `secrets.properties` into the same folder as the Jar file
4. Launch it:
   - Or directly: `java -jar HypixelTimerApp.jar`

## Profit tracking notes

- Profit is calculated the moment a forge timer completes, using live
  Bazaar prices.
- Use the **Profits** screen to see pending count and per-recipe totals.

## Features

- Live forge tracking from the Hypixel API
- Kat pet upgrade timers (auto-detected from your profile)
- Custom reusable timers
- Bazaar price tracking and profit-per-hour calculator
- System tray notifications + sound alerts
- Dark / light theme

## Contact

slivvybiz@gmail.com
