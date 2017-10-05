package org.geoserver.printng.rest;

import java.io.File;
import static org.junit.Assert.*;

import org.geoserver.printng.api.PrintSpec;
import org.junit.Test;

public class RequestPrintSpecTest {

    @Test
    public void testNoDimensionsSpecified() {
        PrintSpec spec = new PrintSpec(null);
        assertFalse(spec.isOutputDimensionSet());
        assertFalse(spec.isRenderDimensionSet());

        spec.useDefaultRenderDimension();
        assertTrue(spec.isRenderDimensionSet());
        assertTrue(spec.isOutputDimensionSet());
        
        spec = new PrintSpec(null);
        spec.setOutputHeight(50);
        spec.setOutputWidth(100);
        assertTrue(spec.isOutputDimensionSet());
        assertFalse(spec.isRenderDimensionSet());
        
        spec.useDefaultRenderDimension();
        assertTrue(spec.isRenderDimensionSet());
        assertEquals(50, spec.getOutputHeight());
        assertEquals(100, spec.getOutputWidth());
    }

    @Test
    public void testDefaultCacheDir() {
        PrintSpec spec = new PrintSpec(null);
        assertNotNull(spec.getCacheDir());

        File root = new File("foobar").getAbsoluteFile();
        spec.setCacheDirRoot(root);
        assertEquals(root, spec.getCacheDir());
    }

    @Test
    public void testCookies() {
        PrintSpec spec = new PrintSpec(null);
        spec.addCookie("foobar", "cookie1", "value");
        spec.addCookie("barfoo", "cookie2", "value");
        
        assertEquals("cookie1", spec.getCookie("foobar").getName());
        try {
            spec.addCookie("foobar", "cookie1", "value");
            fail("expected error on same host cookie");
        } catch (Exception ex) {
            
        }
    }
    
    @Test
    public void testAuth() {
        PrintSpec spec = new PrintSpec(null);
        spec.addCredentials("foobar", "user1", "pass");
        spec.addCredentials("barfoo", "user2", "pass");
        
        assertNotNull(spec.getCredentials("foobar"));
        assertNotNull(spec.getCredentials("barfoo"));
        try {
            spec.addCredentials("foobar", "user1", "pass");
            fail("expected error on same host credentials");
        } catch (Exception ex) {
            
        }
    }
}
