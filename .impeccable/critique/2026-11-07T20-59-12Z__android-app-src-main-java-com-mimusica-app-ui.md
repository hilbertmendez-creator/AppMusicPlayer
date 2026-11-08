---
target: Android app UI (Mi Música)
total_score: 25
max_score: 40
na_heuristics: 
p0_count: 1
p1_count: 3
target_identity: "file:C:\\Users\\Hilbert\\source\\repos\\AppMusicPlayer\\android\\app\\src\\main\\java\\com\\mimusica\\app\\ui"
timestamp: 2026-11-07T20-59-12Z
slug: android-app-src-main-java-com-mimusica-app-ui
---
# Critique: Mi Música Android UI (2026-11-07)
Method: dual-agent (A design review, B mechanical evidence). Degraded: native target, no browser overlay; detector has no Kotlin rules (0 findings = not checked).

## Heuristics: 25/40
1 Status 2 (cold-start splash 8-22s + ANR on emulator; indeterminate download spinner) | 2 Real world 3 (PIN/Clave/Sincronizar jargon) | 3 Control 3 (Borrar adjacent to Cantar) | 4 Consistency 3 (Cantar icon lyrics on Home vs mic elsewhere; green means 3 things) | 5 Error prevention 2 | 6 Recognition 3 (tap-card-to-play invisible) | 7 Efficiency 2 (no continue-listening) | 8 Minimalist 3 (playlist detail 5 equal buttons) | 9 Recovery 3 | 10 Help 1

## Specificity
Structure fits the user; look is generic Material blue/white; brand absent; no music/family warmth.

## Mechanical (B)
Contrast all AAA except disabled "Sincronizando…" 2.46:1. Text >=22sp everywhere. Targets >=64dp (48dp checkbox inside 80dp row). 0 gestures. 0 heading() semantics. ~5 unannounced state changes (search, download, banners). 15 maxLines clip sites without ellipsis.

## Priority issues
- P0 (provisional, verify on real device): cold start looks broken (splash 8-22s, ANR). optimize/harden
- P1: Songs card has no visible Reproducir; Borrar full-width under primary. layout (SongsScreen.kt)
- P1: Now Playing clips Cantar below fold at normal scale. adapt (NowPlayingScreen.kt)
- P1: text size behind caregiver PIN; settings mix caregiver config. distill (SettingsScreens.kt)
- P2: wait/error states lack progress, reasons, success payoff, TalkBack announcements. clarify/harden

## Personas
Rosa: long silent start; can't find how to listen; Borrar near Cantar; PIN jargon. First-timer: no onboarding; empty states without action buttons. TalkBack: no headings, silent progress bar, unmerged cards.

## Minor
Green semantics, cream status bar vs white body, weak type hierarchy, Nueva lista dialog primary not distinct, karaoke has no ending ritual.

## Evidence corrections
06/07 "no results" = capture-script tap on title, not an app bug. Unlocked Settings not captured.
