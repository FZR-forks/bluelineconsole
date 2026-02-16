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
        assertNull(AppSearchActionQueryParser.parse("!wa"));
    }

    @Test
    public void parseRejectsLegacySyntaxWithoutPrefix() {
        assertNull(AppSearchActionQueryParser.parse("youtube cats videos"));
    }

    @Test
    public void parseSplitsAppSelectorAndSearchTextBangSyntax() {
        AppSearchActionQueryParser.ParsedQuery parsedQuery = AppSearchActionQueryParser.parse("youtube cats videos");
        assertNull(parsedQuery);

        parsedQuery = AppSearchActionQueryParser.parse("!wa mom");

        assertNotNull(parsedQuery);
        assertEquals("wa", parsedQuery.appSelector);
        assertEquals("mom", parsedQuery.searchText);
    }
}
