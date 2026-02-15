package net.nhiroki.bluelineconsole.commandSearchers.lib;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
    }

    @Test
    public void parseSplitsAppSelectorAndSearchText() {
        AppSearchActionQueryParser.ParsedQuery parsedQuery = AppSearchActionQueryParser.parse("youtube cats videos");

        assertNotNull(parsedQuery);
        assertEquals("youtube", parsedQuery.appSelector);
        assertEquals("cats videos", parsedQuery.searchText);
    }
}
