package org.opengeo.data.importer.bdb;

import java.io.File;
import java.util.Iterator;
import org.opengeo.data.importer.Directory;
import org.opengeo.data.importer.ImportContext;
import org.opengeo.data.importer.ImportStore.ImportVisitor;
import org.opengeo.data.importer.Importer;
import org.opengeo.data.importer.ImporterTestSupport;


public class BDBImportStoreTest extends ImporterTestSupport {

    BDBImportStore store;
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        
        store = new BDBImportStore(importer);
        store.init();
    }
    
    // in order to test this, run once, then change the serialVersionUID of ImportContext2
    public void testSerialVersionUIDChange() throws Exception {
        Importer imp =  new Importer(null) {

            @Override
            public File getImportRoot() {
                File root = new File("target");
                root.mkdirs();
                return root;
            }
            
        };
        ImportContext ctx = new ImportContext2();
        ctx.setState(ImportContext.State.PENDING);
        ctx.setUser("fooboo");
        store = new BDBImportStore(imp);
        store.init();
        store.add(ctx);
        
        Iterator<ImportContext> iterator = store.iterator();
        while (iterator.hasNext()) {
            ctx = iterator.next();
            assertEquals("fooboo", ctx.getUser());
        }
        
        store.add(ctx);
        
        store.destroy();
    }
    
    public static class ImportContext2 extends ImportContext {
        private static final long serialVersionUID = 12345;
    }

    public void testAdd() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        ImportContext context = importer.createContext(new Directory(dir));

        assertEquals(1,context.getTasks().size());
        for (int i = 0; i < context.getTasks().size(); i++) {
            assertNotNull(context.getTasks().get(i).getStore());
            assertNotNull(context.getTasks().get(i).getStore().getCatalog());
        }
        
        // @todo commented these out as importer.createContext adds to the store
//        assertNull(context.getId());

        CountingVisitor cv = new CountingVisitor();
//        store.query(cv);
//        assertEquals(0, cv.getCount());

        store.add(context);
        assertNotNull(context.getId());
        assertNotNull(context.getTasks().get(0).getItems().get(0).getLayer());

        ImportContext context2 = store.get(context.getId());
        assertNotNull(context2);
        assertEquals(context.getId(), context2.getId());

        store.query(cv);
        assertEquals(1, cv.getCount());
        
        SearchingVisitor sv = new SearchingVisitor(context.getId());
        store.query(sv);
        assertTrue(sv.isFound());
        
        importer.reattach(context2);

        // ensure various transient bits are set correctly on deserialization
        assertEquals(1,context2.getTasks().size());
        for (int i = 0; i < context2.getTasks().size(); i++) {
            assertNotNull(context2.getTasks().get(i).getStore());
            assertNotNull(context2.getTasks().get(i).getStore().getCatalog());
        }
        assertNotNull(context2.getTasks().get(0).getItems().get(0).getLayer());
    }

    public void testSave() throws Exception {
        testAdd();

        ImportContext context = store.get(0);
        assertNotNull(context);

        assertEquals(ImportContext.State.READY, context.getState());
        context.setState(ImportContext.State.COMPLETE);

        ImportContext context2 = store.get(0);
        assertNotNull(context2);
        assertEquals(ImportContext.State.READY, context2.getState());

        store.save(context);
        context2 = store.get(0);
        assertNotNull(context2);
        assertEquals(ImportContext.State.COMPLETE, context2.getState());
    }

    public void testDatabaseRecovery() throws Exception {
        
    }
    
    public void testIncrementId() throws Exception {
        store.advanceId(666L);
        
        // the next created import should be one higher
        ImportContext dumby = new ImportContext();
        store.add(dumby);
        assertEquals(new Long(667), dumby.getId());
    }

    class SearchingVisitor implements ImportVisitor {
        long id;
        boolean found = false;

        SearchingVisitor(long id) {
            this.id = id;
        }
        public void visit(ImportContext context) {
            if (context.getId().longValue() == id) {
                found = true;
            }
        }
        public boolean isFound() {
            return found;
        }
    }

    @Override
    protected void tearDownInternal() throws Exception {
        super.tearDownInternal();
        store.destroy();

//        Environment env = db.getEnvironment();
//        db.close();
//        classDb.close();
//        env.close();
    }
}
