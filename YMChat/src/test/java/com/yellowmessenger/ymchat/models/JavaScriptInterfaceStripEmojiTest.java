package com.yellowmessenger.ymchat.models;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for the emoji filter applied before text reaches TextToSpeech (ISS-16408).
 *
 * Pure string handling, so it runs on the JVM with no device or TTS engine.
 */
public class JavaScriptInterfaceStripEmojiTest {

    @Test
    public void stripsEmojiFromTheReportedCase() {
        assertEquals("Your payment is confirmed",
                JavaScriptInterface.stripEmoji("Your payment is confirmed 😊"));
    }

    @Test
    public void stripsEmojiAtAnyPosition() {
        assertEquals("Hello there", JavaScriptInterface.stripEmoji("😊 Hello there"));
        assertEquals("Great job keep going",
                JavaScriptInterface.stripEmoji("Great job 👍 keep going"));
    }

    /** The trap: emoji-adjacent properties also cover the ASCII digits, '#' and '*'. */
    @Test
    public void leavesDigitsAndAsciiSymbolsAlone() {
        assertEquals("2 items in cart", JavaScriptInterface.stripEmoji("2 items in cart"));
        assertEquals("Call #5 or press *9", JavaScriptInterface.stripEmoji("Call #5 or press *9"));
        assertEquals("Pay 100% now", JavaScriptInterface.stripEmoji("Pay 100% now"));
        assertEquals("Order 1234567890 shipped",
                JavaScriptInterface.stripEmoji("Order 1234567890 shipped"));
    }

    @Test
    public void consumesWholeSequencesWithoutLeavingJoiners() {
        // ZWJ family
        assertEquals("Family plan", JavaScriptInterface.stripEmoji(
                "Family 👨‍👩‍👧‍👦 plan"));
        // skin-tone modifier
        assertEquals("Thumbs up",
                JavaScriptInterface.stripEmoji("Thumbs 👍🏽 up"));
        // regional-indicator flag
        assertEquals("Flag here",
                JavaScriptInterface.stripEmoji("Flag 🇮🇳 here"));
        // with and without the variation selector
        assertEquals("Heart and", JavaScriptInterface.stripEmoji("Heart ❤️ and ❤"));
    }

    /** Keycaps take the base character with them, so no bare "1" is left to read. */
    @Test
    public void stripsKeycapsIncludingTheirBaseCharacter() {
        assertEquals("Step then",
                JavaScriptInterface.stripEmoji("Step 1️⃣ then 2️⃣"));
    }

    @Test
    public void tidiesTheGapsLeftBehind() {
        assertEquals("Done.", JavaScriptInterface.stripEmoji("Done 😊."));
        assertEquals("Wait, then go", JavaScriptInterface.stripEmoji("Wait 🤔 , then go"));
        assertEquals("hello world", JavaScriptInterface.stripEmoji("hello🙂world"));
        assertEquals("Nice! Very nice",
                JavaScriptInterface.stripEmoji("Nice!  😀  Very nice"));
    }

    /** Voices pause on newlines, so they must survive the tidy-up. */
    @Test
    public void keepsNewlines() {
        assertEquals("Line one\nLine two",
                JavaScriptInterface.stripEmoji("Line one 😀\nLine two 🎉"));
    }

    @Test
    public void returnsEmptyWhenNothingSpeakableRemains() {
        assertEquals("", JavaScriptInterface.stripEmoji("😀😀😀"));
        assertEquals("", JavaScriptInterface.stripEmoji("🎉"));
        assertEquals("", JavaScriptInterface.stripEmoji("   "));
        assertEquals("", JavaScriptInterface.stripEmoji(null));
    }

    @Test
    public void leavesOrdinaryTextUntouched() {
        assertEquals("No emoji here at all.",
                JavaScriptInterface.stripEmoji("No emoji here at all."));
        assertEquals("Café naïve résumé",
                JavaScriptInterface.stripEmoji("Café naïve résumé"));
        assertEquals("Ünicode ok — dashes too",
                JavaScriptInterface.stripEmoji("Ünicode ok — dashes too"));
    }

    /** Conservative by design: these read as text far more often than as emoji. */
    @Test
    public void keepsCopyrightRegisteredAndTrademark() {
        assertEquals("© 2026 Acme®, Widget™",
                JavaScriptInterface.stripEmoji("© 2026 Acme®, Widget™"));
    }

    /** Already-stripped text must pass through unchanged — the widget strips first. */
    @Test
    public void isIdempotent() {
        String once = JavaScriptInterface.stripEmoji("All set 😊, see you 👋");
        assertEquals(once, JavaScriptInterface.stripEmoji(once));
    }
}
