---
target: Android app UI (Mi Música)
total_score: 27
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 2
target_identity: "file:C:\\Users\\Hilbert\\source\\repos\\AppMusicPlayer\\android\\app\\src\\main\\java\\com\\mimusica\\app\\ui"
timestamp: 2026-11-07T21-54-12Z
slug: android-app-src-main-java-com-mimusica-app-ui
---
# Critique #2: Mi Música Android UI (2026-11-07, after layout/adapt/distill/optimize/polish)
Method: dual-agent (A fresh design review, B mechanical evidence). Degraded: native target, no browser overlay; detector cannot analyze Kotlin. Evidence: Pixel 7a screenshots in .impeccable/critique/screens-v2/.

## Heuristics: 27/40 (prev 25)
1 Status 3 | 2 Real world 3 (PIN/número secreto drift; "Para quien ayuda" reads as help) | 3 Control 3 | 4 Consistency 3 | 5 Error prevention 3 | 6 Recognition 3 (icon-only transport) | 7 Efficiency 2 (no continue/play-all) | 8 Minimalist 3 (song card ~950px) | 9 Recovery 3 | 10 Help 1

## Mechanical (B)
Text contrast all >=7:1 (min 7.62). Sizes >=22sp; targets >=64dp; 0 gestures. Progress track vs page 1.08:1 (regression from last fix). 9 silent state changes (no liveRegion). RowCards unmerged on 4 screens. 5 maxLines without ellipsis. App text scale stacks on OS fontScale with no cap.

## Priority issues
- P1: download moment: no duration on results, silent indeterminate spinner, no next step (Reproducir ahora / Agregar a lista), unannounced. clarify+harden (SearchScreen.kt:161-171)
- P1: Now Playing icon-only transport, 7 targets, no "Sigue:". layout (NowPlayingScreen.kt:148-170)
- P2: Home tiles shift when mini-player appears (verified 01 vs 04); "Tamaño de letra" looks like caption; "Para quien ayuda" door ambiguous. layout (HomeScreen.kt)
- P2: Mis canciones cards don't scale; no Reproducir todo; Borrar per row. distill (SongsScreen.kt)
- P2: a11y debt: invisible progress track, 9 silent states, unmerged cards, uncapped text scale. harden

## Personas
Rosa: shifting tiles, pause inside tappable mini-player, delete dialog lacks "can re-download", unsure if right song. Caregiver: setup lacks examples/test connection. TalkBack: karaoke silent, Volver top-left.

## Minor
Cantar empty state dead end; 5-result cap; "Sí, borrar" first; PIN length hint; 3 unused strings (mini_player_open missing as label).
