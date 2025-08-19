
# AlteWeserWind (Widget)

Simple Android app + homescreen widget that shows wind speed for **Leuchtturm Alte Weser** from PEGELONLINE, with Beaufort, km/h, kn, and a direction arrow (arrow points **to** where wind is going; degrees show **from** direction).

## Build via GitHub Actions (no Android Studio needed)
1. Create a new **public** repo on GitHub.
2. Upload this entire folder (`AlteWeserWind/` at repo root).
3. Push to `main` (or use **Upload files** in the web UI).
4. Go to **Actions** tab → open the latest run → **Artifacts** → download `AlteWeserWind-debug-apk` → install `app-debug.apk` on your phone.
   - You may need to allow installs from your browser/file manager.

## Widget
- Long-press homescreen → **Widgets** → *Alte Weser Wind*.
- Updates automatically every **30 minutes** (WorkManager), and also when you add the widget or reboot.
- Tap the widget to open the full-screen view (the same HTML with 60s auto-refresh and debug).

## Notes
- Station UUID: `c6772c3c-a6bb-4728-9250-a408ab3856bd`
- Fetch URL: `https://www.pegelonline.wsv.de/webservices/rest-api/v2/stations/{uuid}.json?includeTimeseries=true&includeCurrentMeasurement=true`
- The widget uses an 8-way arrow (↑ ↗ → ↘ ↓ ↙ ← ↖) mapped from the **to** direction (deg + 180°).

## Colors (Beaufort)
- 0–2: light blue `#60a5fa`
- 3–4: green `#22c55e`
- 5: yellow `#eab308`
- 6: orange `#f97316`
- 7–9: red `#ef4444`
- 10–12: dark red-purple `#a21caf`

## License
MIT
