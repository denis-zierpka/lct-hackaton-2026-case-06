// Generates the Material 3 colour scheme for Theme.kt from seed colours using
// Google's material-color-utilities (same algorithm as Material Theme Builder).
// Usage: node tools/office/gen_theme.js > app/src/main/java/ru/finny/pet/ui/Colors.kt
const { argbFromHex, hexFromArgb, Hct, SchemeTonalSpot, TonalPalette, MaterialDynamicColors } = require("@material/material-color-utilities");

// 0.2.x exposes the dynamic colours as statics, 0.3.x as instance members; support both.
const MDC = typeof MaterialDynamicColors.primary !== "undefined" ? MaterialDynamicColors : new MaterialDynamicColors();
const role = (scheme, name) => MDC[name].getArgb(scheme);

// Palette of the ЛЦТ-2026 presentation template: purple #520978 / #310F53, accent #FF0053,
// light pink #FFD6E4, lavender #8A83D1, base #FFFFFF / #1C1D22.
const SEED = "#520978";      // brand purple → primary
const SECONDARY = "#FF0053"; // accent magenta → savings, goal, highlights (container ≈ #FFD6E4)
const TERTIARY = "#8A83D1";  // lavender → tasks, care

// Neutrals are set by hand: white cards on a cool light-grey ground (template look) instead of
// the pinkish tint SchemeTonalSpot derives from a purple seed; dark ground = template #1C1D22.
const NEUTRALS = {
  light: {
    background: "#F4F2F8", onBackground: "#1C1D22", surface: "#F4F2F8", onSurface: "#1C1D22",
    surfaceVariant: "#E5E1EC", onSurfaceVariant: "#4B4653", outline: "#7B7684", outlineVariant: "#CDC8D6",
    surfaceBright: "#F4F2F8", surfaceDim: "#D8D5DF",
    surfaceContainerLowest: "#FFFFFF", surfaceContainerLow: "#FFFFFF", surfaceContainer: "#ECE9F2",
    surfaceContainerHigh: "#E5E1EC", surfaceContainerHighest: "#DEDAE6",
    inverseSurface: "#2D2C33", inverseOnSurface: "#F2F0F5",
  },
  dark: {
    background: "#16151B", onBackground: "#E6E3EC", surface: "#16151B", onSurface: "#E6E3EC",
    surfaceVariant: "#4B4653", onSurfaceVariant: "#CDC8D6", outline: "#96919F", outlineVariant: "#4B4653",
    surfaceBright: "#3C3B43", surfaceDim: "#16151B",
    surfaceContainerLowest: "#0F0E13", surfaceContainerLow: "#1F1E25", surfaceContainer: "#24232B",
    surfaceContainerHigh: "#2E2D35", surfaceContainerHighest: "#39383F",
    inverseSurface: "#E6E3EC", inverseOnSurface: "#2D2C33",
  },
};

const primary = TonalPalette.fromHct(Hct.fromInt(argbFromHex(SEED)));
const secondary = TonalPalette.fromHct(Hct.fromInt(argbFromHex(SECONDARY)));
const tertiary = TonalPalette.fromHct(Hct.fromInt(argbFromHex(TERTIARY)));

function tokens(dark) {
  const s = new SchemeTonalSpot(Hct.fromInt(argbFromHex(SEED)), dark, 0.0);
  // M3 tone mapping for accent roles (light: 40/100/90/10, dark: 80/20/30/90)
  const t = dark ? { base: 80, on: 20, container: 30, onContainer: 90 } : { base: 40, on: 100, container: 90, onContainer: 10 };
  const r = (name) => role(s, name);
  // Brand accents are used verbatim where contrast allows: #520978 on white ≈ 11:1;
  // the template's #FF0053 (tone 54) is only 3.9:1 on white, so text/icon uses tone 45 (#D50044).
  const primaryBase = dark ? primary.tone(80) : argbFromHex(SEED);
  const secondaryBase = dark ? secondary.tone(80) : secondary.tone(45);
  // Dark container: same hue, chroma halved — tone-30 magenta at full chroma reads as an error red.
  const secondaryContainer = dark ? Hct.from(Hct.fromInt(argbFromHex(SECONDARY)).hue, 40, 32).toInt() : argbFromHex("#FFD6E4");
  const n = Object.fromEntries(Object.entries(NEUTRALS[dark ? "dark" : "light"]).map(([k, v]) => [k, argbFromHex(v)]));
  return {
    primary: primaryBase, onPrimary: primary.tone(t.on), primaryContainer: primary.tone(t.container), onPrimaryContainer: primary.tone(t.onContainer),
    inversePrimary: primary.tone(dark ? 40 : 80),
    secondary: secondaryBase, onSecondary: secondary.tone(t.on), secondaryContainer, onSecondaryContainer: secondary.tone(t.onContainer),
    tertiary: tertiary.tone(t.base), onTertiary: tertiary.tone(t.on), tertiaryContainer: tertiary.tone(t.container), onTertiaryContainer: tertiary.tone(t.onContainer),
    error: r("error"), onError: r("onError"), errorContainer: r("errorContainer"), onErrorContainer: r("onErrorContainer"),
    background: n.background, onBackground: n.onBackground,
    surface: n.surface, onSurface: n.onSurface, surfaceVariant: n.surfaceVariant, onSurfaceVariant: n.onSurfaceVariant,
    surfaceTint: primaryBase, inverseSurface: n.inverseSurface, inverseOnSurface: n.inverseOnSurface,
    outline: n.outline, outlineVariant: n.outlineVariant, scrim: r("scrim"),
    surfaceBright: n.surfaceBright, surfaceDim: n.surfaceDim,
    surfaceContainer: n.surfaceContainer, surfaceContainerHigh: n.surfaceContainerHigh, surfaceContainerHighest: n.surfaceContainerHighest,
    surfaceContainerLow: n.surfaceContainerLow, surfaceContainerLowest: n.surfaceContainerLowest,
  };
}

function kotlin(name, fn, t) {
  const lines = Object.entries(t).map(([k, v]) => `    ${k} = Color(0xFF${hexFromArgb(v).slice(1).toUpperCase()}),`);
  return `val ${name} = ${fn}(\n${lines.join("\n")}\n)`;
}

console.log(`package ru.finny.pet.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Generated by tools/office/gen_theme.js from seed ${SEED} (SchemeTonalSpot),
// secondary ${SECONDARY}, tertiary ${TERTIARY}. Do not edit by hand.

${kotlin("LightScheme", "lightColorScheme", tokens(false))}

${kotlin("DarkScheme", "darkColorScheme", tokens(true))}
`);
