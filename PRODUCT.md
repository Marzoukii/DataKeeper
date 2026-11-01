# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users
Tunisians on prepaid mobile plans (Ooredoo, Orange, Tunisie Telecom), often young or on a tight budget, who recharge fixed-size data passes with limited validity (e.g. "25 Go valable 30 jours") and want to keep them until the end. They check the app in short glances: in the morning, before watching a video on mobile data, or when a quota alert fires.

## Product Purpose
DataKeeper gives a complete view and control of data use on the phone: how much mobile data is left on the real plan, Wi‑Fi versus mobile split, which apps eat the plan, a daily budget and an end-of-cycle forecast, plus tools to save data (Wi‑Fi-only downloads, open Wi‑Fi nearby, speed test, tethering usage). Success: the user always knows where they stand and never runs dry by surprise.

## Positioning
Built around how Tunisian prepaid plans actually work, not the 1st of the month: plan size + purchase day + validity, a balance recalibrated from the operator's own figure, prices in DT, savings computed in dinars, and SIM changes detected automatically. Everything stays on the phone.

## Operating Context
- Root flow: usage-access permission (with an Android 13+/15 "restricted settings" help path) → plan setup (size chips, remaining balance, optional price, validity/cycle, saved plans per operator, "no mobile plan" skip) → main app.
- Five bottom tabs, which must be kept: Conso, Quota, Hors ligne, Débit, Wi‑Fi. Sub-screens: Paramètres (theme, language, about) and Partage de connexion.
- Home surfaces: three widgets (quota ring, quota bar with projection, Wi‑Fi savings) and quick-settings tiles; notifications for quota threshold, plan renewal and balance recalibration.
- Languages: French (default), English, Arabic (RTL).

## Capabilities and Constraints
- Reads only volumes from Android's usage stats; never content. No data leaves the phone.
- Jetpack Compose, Material 3, light and dark themes, MVI view models.
- Currency DT with 3 decimals (e.g. 1,500 DT). Units Mo / Go.
- Open Wi‑Fi list requires location permission; distinguishes captive portals with ads.

## Brand Commitments
- Name "DataKeeper" is binding. Logo, colors and typography may be replaced (redesign approved 2026-10-31).
- Existing copy uses informal "tu", direct and concrete; keep the voice unless asked.

## Evidence on Hand
- All UI copy in `app/src/main/res/values/strings.xml` (FR), `values-en`, `values-ar`.
- No testimonials, ratings, user counts or press. Do not invent any.

## Product Principles
1. Where you stand, in one glance: remaining data and days come first on every relevant surface.
2. Follow the real plan, not the calendar.
3. Concrete advice over raw numbers ("Encore 420 Mo pour aujourd'hui").
4. Private by construction; say so where permissions are asked.
5. Every tool serves the same goal: keep mobile data for when it matters.

## Accessibility & Inclusion
Must work in Arabic RTL (no negative letter-spacing on Arabic), with large font scales and in dark mode; charts carry text descriptions.
