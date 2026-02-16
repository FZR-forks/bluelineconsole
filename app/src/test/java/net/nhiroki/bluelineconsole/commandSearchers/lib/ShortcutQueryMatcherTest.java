package net.nhiroki.bluelineconsole.commandSearchers.lib;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ShortcutQueryMatcherTest {
    @Test
    public void matchScorePrefersShortLabel() {
        int score = ShortcutQueryMatcher.matchScore(
                "ali",
                "Alice",
                "Message Alice",
                "WhatsApp",
                "com.whatsapp"
        );

        assertEquals(0, score);
    }

    @Test
    public void matchScoreFallsBackToLongLabel() {
        int score = ShortcutQueryMatcher.matchScore(
                "message bob",
                "Bob",
                "Message Bob",
                "WhatsApp",
                "com.whatsapp"
        );

        assertEquals(1000, score);
    }

    @Test
    public void matchScoreFallsBackToAppLabelAndPackageName() {
        int appLabelScore = ShortcutQueryMatcher.matchScore(
                "whats",
                "Alice",
                "Message Alice",
                "WhatsApp",
                "com.whatsapp"
        );
        int packageScore = ShortcutQueryMatcher.matchScore(
                "com.wh",
                "Alice",
                "Message Alice",
                "WhatsApp",
                "com.whatsapp"
        );

        assertEquals(2000, appLabelScore);
        assertEquals(3000, packageScore);
    }

    @Test
    public void matchScoreReturnsNegativeForNoMatch() {
        int score = ShortcutQueryMatcher.matchScore(
                "telegram",
                "Alice",
                "Message Alice",
                "WhatsApp",
                "com.whatsapp"
        );

        assertTrue(score < 0);
    }
}
