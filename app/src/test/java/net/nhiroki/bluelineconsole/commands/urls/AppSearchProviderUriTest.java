package net.nhiroki.bluelineconsole.commands.urls;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AppSearchProviderUriTest {
    @Test
    public void fromPackageNameAndGetPackageNameRoundTrip() {
        String uri = AppSearchProviderUri.fromPackageName("com.whatsapp");

        assertTrue(AppSearchProviderUri.isAppSearchProviderUri(uri));
        assertEquals("com.whatsapp", AppSearchProviderUri.getPackageName(uri));
    }

    @Test
    public void invalidUriIsRejected() {
        assertFalse(AppSearchProviderUri.isAppSearchProviderUri("https://example.com"));
        assertFalse(AppSearchProviderUri.isAppSearchProviderUri("bluelineconsole-app-search://"));
    }
}
