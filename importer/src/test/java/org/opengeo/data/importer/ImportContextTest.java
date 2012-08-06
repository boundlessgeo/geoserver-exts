package org.opengeo.data.importer;

import junit.framework.TestCase;

public class ImportContextTest extends TestCase {

    public void testUpdateState() throws Exception {
        ImportContext context = new ImportContext(0);

        ImportTask t1 = new ImportTask(null);
        ImportTask t2 = new ImportTask(null);
        ImportTask t3 = new ImportTask(null);
        context.addTask(t1);
        context.addTask(t2);
        context.addTask(t3);

        assertEquals(ImportContext.State.PENDING, context.getState());

        t1.setState(ImportTask.State.READY);
        context.updated();
        assertEquals(ImportContext.State.INCOMPLETE, context.getState());
        
        t2.setState(ImportTask.State.READY);
        t3.setState(ImportTask.State.READY);
        context.updated();
        assertEquals(ImportContext.State.READY, context.getState());

        t1.setState(ImportTask.State.COMPLETE);
        t2.setState(ImportTask.State.COMPLETE);
        context.updated();
        assertEquals(ImportContext.State.READY, context.getState());
        
        t3.setState(ImportTask.State.COMPLETE);
        context.updated();
        assertEquals(ImportContext.State.COMPLETE, context.getState());
    }
}
