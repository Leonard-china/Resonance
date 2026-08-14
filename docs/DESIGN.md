# Resonance design direction

## Visual thesis

A quiet midnight record room: near-black blue surfaces, warm ivory type, and one coral signal color. Album art carries the emotion; interface chrome stays restrained.

## Interaction thesis

- Navigation transitions are short fades with small shared-axis movement (180–240 ms).
- Album artwork expands into the player instead of opening a disconnected page.
- Playback and sync state use subtle continuous motion only while work is active.

## Layout rules

- Compact (< 720 dp): bottom navigation, edge-to-edge content, persistent mini-player.
- Medium (720–1099 dp): navigation rail, two-column library where useful.
- Expanded (>= 1100 dp): fixed sidebar, content workspace, optional now-playing inspector.
- Use an 8 dp spacing rhythm and 48 dp minimum interactive targets.
- Avoid generic card grids; cards exist only when the whole surface is interactive.

## Color tokens

- Canvas: `#0D1117`
- Raised surface: `#151B23`
- Soft surface: `#1C2430`
- Primary text: `#F7F2E8`
- Secondary text: `#A9B1BD`
- Accent: `#FF735C`
- Positive: `#5FD19B`
- Warning: `#F2BD5B`

## Typography

Use the platform's high-quality system sans family initially. Titles are compact and confident; body text remains neutral and highly legible. A bundled open font may be added only after Chinese glyph coverage and package cost are verified.

## Performance and accessibility gates

- No file scanning, hashing, decoding, or artwork extraction on the UI thread.
- Avoid blur-heavy effects and unbounded artwork decoding.
- All icon-only controls require semantic labels/tooltips.
- Respect reduced-motion preferences where available.
- Verify compact and expanded screenshots for every significant UI change.
