package org.opengeo.data.importer.transform;

import java.beans.PropertyDescriptor;
import java.io.StringWriter;
import junit.framework.TestCase;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONBuilder;
import org.opengeo.data.importer.rest.ImportJSONIO;
import org.springframework.beans.BeanUtils;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public abstract class TransformTestSupport extends TestCase {

    public void doJSONTest(ImportTransform transform) throws Exception {
        StringWriter buffer = new StringWriter();
        JSONBuilder builder = new JSONBuilder(buffer);
        ImportJSONIO jsonio = ImportJSONIO.createUnitTestImportJSONIO();
        jsonio.importTransform(transform, builder);
        JSONObject json = JSONObject.fromObject(buffer.toString());
        ImportTransform transform2 = jsonio.importTransform(json);
        PropertyDescriptor[] pd = BeanUtils.getPropertyDescriptors(transform.getClass());
        for (int i = 0; i < pd.length; i++) {
            assertEquals("expected same value of " + pd[i].getName(),
                    pd[i].getReadMethod().invoke(transform),
                    pd[i].getReadMethod().invoke(transform2));
        }
    }
}
