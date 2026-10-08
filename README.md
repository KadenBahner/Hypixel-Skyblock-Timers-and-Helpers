# Hypixel-Skyblock-Timers-and-Helpers
Full companion app for hypixel skyblock, including full forge connection to set timers, as well as a forge profit calculator. It also includes a kat pet upgrade finder, stat tracking, and more!


# Hypixel Skyblock Timer

A desktop timer for Hypixel Skyblock forges, Kat pet upgrades, and any custom
grind. Tracks live Bazaar margins and records profit each time a forge timer
completes.

## Requirements

- - **Java 25** — [Download Eclipse Temurin 25](https://adoptium.net/temurin/releases/?version=25)
- A free [Hypixel developer API key](https://developer.hypixel.net/)

## Setup

1. Open `secrets.properties`
2. Paste your Hypixel API key into it
3. Build the JAR from Eclipse:
   - Right-click the project → Export → Runnable JAR file
   - Launch configuration: **HypixelTimerApp**
   - Export destination: `HypixelTimerApp.jar` in the project root
   - Library handling: **Extract required libraries into generated JAR**
4. Launch it:
   - Or directly: `java -jar HypixelTimerApp.jar`

## Profit tracking notes

- Profit is calculated the moment a forge timer completes, using live
  Bazaar prices. It's a point-in-time snapshot, not a realized gain.
- If prices are unavailable when a timer completes (offline, API down),
  the record is queued and applied as soon as prices load. Pending records
  survive restarts.
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
