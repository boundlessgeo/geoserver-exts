/* Copyright (c) 2014 - 2015 Boundless http://boundlessgeo.com - all rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package com.boundlessgeo.suite.geoserver.intruments;

import com.amazonaws.services.cloudwatch.model.MetricDatum;
import java.util.Collection;
//import org.aopalliance.intercept.MethodInvocation;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 * @author tingold@boundlessgeo.com
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:test-context.xml")
public class OWSServiceInstrumentTest {
    
    public OWSServiceInstrumentTest() {
    }
   
    @Autowired
    private OWSServiceInstrument instance;
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of afterPropertiesSet method, of class OWSServiceInstrument.
     */
    @Test
    public void testAfterPropertiesSet() {
        System.out.println("afterPropertiesSet");
        
        instance.afterPropertiesSet();
    }

    

    /**
     * Test of getMetrics method, of class OWSServiceInstrument.
     */
    /*@Test
    public void testGetMetrics() {
        System.out.println("getMetrics");
        
        Collection<MetricDatum> expResult = null;
        Collection<MetricDatum> result = instance.getMetrics();
        assertEquals(expResult, result);
    }*/

    /**
     * Test of getServiceName method, of class OWSServiceInstrument.
     */
    @Test
    public void testGetServiceName() {
        System.out.println("getServiceName");
        
        String expResult = "wms";
        String result = instance.getServiceName();
        assertEquals(expResult, result);      
    }

    /**
     * Test of setServiceName method, of class OWSServiceInstrument.
     */
    @Test
    public void testSetServiceName() {
        System.out.println("setServiceName");
        String oldServiceName = instance.getServiceName();
        String serviceName = "";
        
        instance.setServiceName(serviceName);

        //restore previous service name
        instance.setServiceName(oldServiceName);

    }

    /**
     * @return the instance
     */
    public OWSServiceInstrument getInstance() {
        return instance;
    }

    /**
     * @param instance the instance to set
     */
    public void setInstance(OWSServiceInstrument instance) {
        this.instance = instance;
    }
    
}
