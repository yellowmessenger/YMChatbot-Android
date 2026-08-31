# Changelog

All notable changes to this project will be documented in this file.

## [v3.5.0](https://github.com/yellowmessenger/YMChatbot-Android/releases/tag/v3.5.0) (2026-08-31)

### New Update 🚀
* Added `YMConfig.initialUserMessage`: when set, the SDK automatically sends the configured text as a user message immediately after the chat widget loads, rendering it as a real outgoing chat bubble without the end user needing to type anything.

---

## [v3.4.0](https://github.com/yellowmessenger/YMChatbot-Android/releases/tag/v3.4.0) (2026-08-06)

### New Update 🚀
* Added automatic screen-awake handling during Voice Mode: the SDK now keeps the device screen on for the duration of an active voice conversation (driven by `voice-mode-started`/`voice-mode-ended` events from the web widget) and restores normal screen-timeout behavior as soon as the call ends.

---

## [v3.3.2](https://github.com/yellowmessenger/YMChatbot-Android/releases/tag/v3.3.2) (2026-07-23)

### Security 🔒
* Closed a residual gap from the `v3.3.1` fix: the popup WebView opened via `window.open()`/`target=_blank` (`onCreateWindow`) had no scheme allowlist and did not have `allowFileAccess` disabled, since it's a separate WebView instance that doesn't inherit the main WebView's settings. Now enforces the same `http`/`https`-only scheme allowlist and disables `allowFileAccess` on the popup WebView.

---

## [v3.3.1](https://github.com/yellowmessenger/YMChatbot-Android/releases/tag/v3.3.1) (2026-07-23)

### Security 🔒
* Restricted the WebView JS bridge's `loadURL()` to `http`/`https` schemes, closing off `javascript:`, `file:`, `data:`, and `content:` URLs.
* Fixed link handling so `shouldOpenLinkExternally` (default `true`) correctly opens links in the system browser instead of loading them in the in-app WebView.
* Disabled WebView file-system access (`allowFileAccess`) since it is not required for normal operation.

---

## [v3.3.0](https://github.com/yellowmessenger/YMChatbot-Android/releases/tag/v3.3.0) (2026-05-19)

### New Update 🚀
* Introduced `YMUploadSource` enum (`CAMERA`, `FILE`) for explicit, extensible upload source configuration.
* Added `allowedUploadSources: List<YMUploadSource>` to `YMConfig` — supports camera-only, file-only, or both.
* Widget-driven camera capture is now detected automatically via the HTML `<input capture>` attribute — no SDK config required.
* Deprecated `hideCameraForUpload` in favour of `allowedUploadSources`. Existing integrations are fully backward compatible with no changes required.

---

## [v3.2.0](https://github.com/yellowmessenger/YMChatbot-Android/releases/tag/v3.2.0) (2026-05-08)

### New Update 🚀
* Added `stopVoiceMode()` public API on `YMChat` to allow host apps to explicitly stop voice mode — useful when presenting a dialog or bottom sheet over the chatbot where `onStop` is not called.

---

## [v1.0.0](https://github.com/yellowmessenger/YMChatbot-Android/releases/tag/v1.0.0)

### Added
- Introducing YMChat library to integrate Yellow Messenger Chatbot in an Android application
