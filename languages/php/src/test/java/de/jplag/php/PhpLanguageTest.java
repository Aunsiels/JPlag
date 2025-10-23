package de.jplag.php;

import de.jplag.testutils.LanguageModuleTest;
import de.jplag.testutils.datacollector.TestDataCollector;
import de.jplag.testutils.datacollector.TestSourceIgnoredLinesCollector;

/**
 * Tests for the PHP language module.
 */
public class PhpLanguageTest extends LanguageModuleTest {

    public PhpLanguageTest() {
        super(new PhpLanguage(), PhpTokenType.class);
    }

    @Override
    protected void collectTestData(TestDataCollector collector) {
        collector.testFile("TokensCoverage.php").testCoverages();
    }

    @Override
    protected void configureIgnoredLines(TestSourceIgnoredLinesCollector collector) {
        collector.ignoreEmptyLines();
        collector.ignoreLinesByContains("=>");
    }
}
