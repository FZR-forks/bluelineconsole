package net.nhiroki.bluelineconsole.commandSearchers.eachSearcher;

import org.junit.Assert;
import org.junit.Test;

public class ApplicationCommandSearcherSortingTest {
    @Test
    public void prioritizeBetterTextMatchBeforeUsage() {
        int result = ApplicationCommandSearcher.compareCandidateScores(
                10, 1_000L, "whatsapp", "com.whatsapp",
                20, 9_999L, "whatsweb", "com.example.whatsweb"
        );

        Assert.assertTrue(result < 0);
    }

    @Test
    public void prioritizeRecentUsageWhenMatchScoreIsSame() {
        int result = ApplicationCommandSearcher.compareCandidateScores(
                10, 9_999L, "whatsweb", "com.example.whatsweb",
                10, 1_000L, "whatsapp", "com.whatsapp"
        );

        Assert.assertTrue(result < 0);
    }

    @Test
    public void fallBackToAlphabeticalTieBreakers() {
        int result = ApplicationCommandSearcher.compareCandidateScores(
                10, 1_000L, "WhatsApp", "com.whatsapp",
                10, 1_000L, "WhatsWeb", "com.example.whatsweb"
        );

        Assert.assertTrue(result < 0);
    }
}
