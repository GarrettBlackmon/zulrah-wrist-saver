# Zulrah Wrist Saver

Outlines **Zulrah** a few ticks after it first becomes visible so you don’t have to spam-click. The outline hides as soon as you attack and resets each phase.

> ⚠️ This plugin does **not** automate inputs. It only draws an outline and follows Jagex/RuneLite 3PC rules.

Video Demo:

[![Demo Video](https://img.youtube.com/vi/foLr4ffOURU/0.jpg)](https://www.youtube.com/watch?v=foLr4ffOURU)

---

## Features
- **Delayed outline** – starts counting on the first on-screen frame; outlines after **4 ticks** (≈2.4s)
- **Auto-hide on attack** – outline is suppressed once your player targets Zulrah
- **Phase-aware** – resets on morphs and dives; re-arms for the next emerge
- **Configurable look** – outline **color** and **feather** (softness)

---

## Configuration
Open the plugin config (wrench icon):
- **Outline color** — color used for the model outline
- **Outline feather** — 0–12; higher = softer edge

*(Outline width is fixed in this MVP.)*

---

## Usage
1. Enable **Zulrah Wrist Saver**.
2. During the fight, when Zulrah surfaces, wait for the outline to appear (after the short delay), then click once.
3. The outline hides after you attack and returns on the next phase.

---

## Install

### Plugin Hub (recommended)
Coming soon — once approved, install from **Plugin Hub** inside RuneLite.

### Local / external testing
Build a distributable ZIP:
```bash
./gradlew clean build
