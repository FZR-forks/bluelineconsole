package net.nhiroki.bluelineconsole.commandSearchers.lib;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AppSearchActionQueryParserTest {
    @Test
    public void parseReturnsNullForEmptyQuery() {
        assertNull(AppSearchActionQueryParser.parse(""));
        assertNull(AppSearchActionQueryParser.parse("   "));
        assertNull(AppSearchActionQueryParser.parse(null));
    }

    @Test
    public void parseReturnsNullForSingleWord() {
        assertNull(AppSearchActionQueryParser.parse("whatsapp"));
        assertNull(AppSearchActionQueryParser.parse("!wa"));
    }

    @Test
    public void parseSplitsAppSelectorAndSearchTextLegacySyntax() {
        AppSearchActionQueryParser.ParsedQuery parsedQuery = AppSearchActionQueryParser.parse("youtube cats videos");

        assertNotNull(parsedQuery);
        assertEquals("youtube", parsedQuery.appSelector);
        assertEquals("cats videos", parsedQuery.searchText);
        assertFalse(parsedQuery.bangSyntax);
    }

    @Test
    public void parseSplitsAppSelectorAndSearchTextBangSyntax() {
        AppSearchActionQueryParser.ParsedQuery parsedQuery = AppSearchActionQueryParser.parse("!wa mom");

        assertNotNull(parsedQuery);
        assertEquals("wa", parsedQuery.appSelector);
        assertEquals("mom", parsedQuery.searchText);
        assertTrue(parsedQuery.bangSyntax);
    }
}
