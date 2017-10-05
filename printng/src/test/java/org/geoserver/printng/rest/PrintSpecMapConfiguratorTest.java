package org.geoserver.printng.rest;

import java.util.logging.Level;
import org.geoserver.printng.api.PrintSpec;
import org.geoserver.printng.rest.PrintSpecMapConfigurator;
import org.geoserver.printng.spi.PrintSpecException;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import testsupport.PrintTestSupport.LogCollector;
import static testsupport.PrintTestSupport.map;

/**
 * While it is a cool idea to verify log messages, this fails when running
 * the whole suite as log4j logger adapters don't allow handlers to be added
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class PrintSpecMapConfiguratorTest {

    PrintSpec spec = new PrintSpec(null);
    private LogCollector records;

    @Before
    public void installHandler() {
        records = LogCollector.attach(PrintSpecMapConfigurator.class, Level.FINE);
    }

    @After
    public void clearHandler() {
        records.detach();
    }

    @Test
    public void testValidation() {
        try {
            PrintSpecMapConfigurator.configure(spec, map("width", "xyz"));
            fail("expected exception");
        } catch (PrintSpecException pse) {
            // make sure it's mentioned somewhere
            assertTrue(pse.getMessage().indexOf("width") >= 0);
        }
        try {
            PrintSpecMapConfigurator.configure(spec, map("height", "xyz"));
            fail("expected exception");
        } catch (PrintSpecException pse) {
            // make sure it's mentioned somewhere
            assertTrue(pse.getMessage().indexOf("height") >= 0);
        }
        try {
            PrintSpecMapConfigurator.configure(spec, map("auth", "1,2"));
            fail("expected exception");
        } catch (PrintSpecException pse) {
            // make sure it's mentioned somewhere
            assertTrue(pse.getMessage().indexOf("auth") >= 0);
        }
        try {
            PrintSpecMapConfigurator.configure(spec, map("cookie", "1,2"));
            fail("expected exception");
        } catch (PrintSpecException pse) {
            // make sure it's mentioned somewhere
            assertTrue(pse.getMessage().indexOf("cookie") >= 0);
        }
    }

    @Test
    public void testMultipleCookies() {
        PrintSpecMapConfigurator.configure(spec, map("cookie", "a,b,c", "cookie", "d,e,f"));
        assertNotNull(spec.getCookie("a"));
        assertNotNull(spec.getCookie("d"));
        // @todo see note in class javadoc
//        assertEquals(2, records.records.size());
    }

    @Test
    public void testMultipleAuth() {
        PrintSpecMapConfigurator.configure(spec, map("auth", "a,b,c", "auth", "d,e,f"));
        assertNotNull(spec.getCredentials("a"));
        assertNotNull(spec.getCredentials("d"));
        // @todo see note in class javadoc
//        assertEquals(2, records.records.size());
    }
}
