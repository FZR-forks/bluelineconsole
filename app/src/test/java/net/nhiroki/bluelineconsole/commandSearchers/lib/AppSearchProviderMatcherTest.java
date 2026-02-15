package net.nhiroki.bluelineconsole.commandSearchers.lib;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AppSearchProviderMatcherTest {
    @Test
    public void matchScoreMatchesLabelFirst() {
        int score = AppSearchProviderMatcher.matchScore("whats", "WhatsApp", "com.whatsapp");
        assertEquals(0, score);
    }

    @Test
    public void matchScoreMatchesPackageIfLabelDoesNotMatch() {
        int score = AppSearchProviderMatcher.matchScore("comwha", "Messenger", "com.whatsapp");
        assertEquals(1000, score);
    }

    @Test
    public void matchScoreMatchesAliasFromFirstTwoChars() {
        int score = AppSearchProviderMatcher.matchScore("ms", "Meta Search", "com.whatsapp");
        assertTrue(score >= 2000);
    }

    @Test
    public void matchScoreReturnsNegativeForNoMatch() {
        int score = AppSearchProviderMatcher.matchScore("px", "WhatsApp", "com.whatsapp");
        assertTrue(score < 0);
    }
}
