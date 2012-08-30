package org.geoserver.printng;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;

import org.geoserver.printng.api.PrintSpec;
import org.geoserver.printng.api.PrintngWriter;
import org.geoserver.printng.spi.DocumentParser;
import org.geoserver.printng.spi.ImageWriter;
import org.geoserver.printng.spi.MapPrintSpec;
import org.geoserver.printng.spi.PDFWriter;
import org.w3c.dom.Document;

import com.google.common.collect.ImmutableMap;

/**
 * Use this as an interactive test driver.
 * 
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class HTMLToPDF {

    public static void main(String[] args) throws Exception {
        int dpp = 20;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        LinkedList<String> argList = new LinkedList<String>(Arrays.asList(args));
        boolean img = argList.remove("-img");
        boolean loop = argList.remove("-loop");
        // no caching yet
        // boolean cache = argList.remove("-cache");
        File inputFile = new File(argList.pop());
        File outputFile;
        PrintngWriter writer;
        if (!argList.isEmpty()) {
            outputFile = new File(argList.pop());
        } else {
            String ext = img ? ".png" : ".pdf";
            outputFile = new File(inputFile.getName().replace(".html", ext));
        }
        while (true) {
            FileReader fileReader = new FileReader(inputFile);
            DocumentParser printngDocumentParser = new DocumentParser(fileReader);
            Document document = printngDocumentParser.parse();
            ImmutableMap<String, ? extends Object> options = ImmutableMap.of("width", 1280,
                    "height", 768, "dpp", dpp);
            PrintSpec printSpec = new MapPrintSpec(options);

            System.out.print("rendering...");
            if (img) {
                writer = new ImageWriter("png");
            } else {
                writer = new PDFWriter();
            }

            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
            writer.write(document, printSpec, fileOutputStream);

            System.out.println("done");
            if (loop) {
                Desktop.getDesktop().open(outputFile);
                System.out.println("press enter to run again: 'q' to quit");
                String line = br.readLine();
                if ("q".equals(line)) {
                    break;
                }
                try {
                    dpp = Integer.parseInt(line);
                } catch (NumberFormatException nfe) {
                }
            } else {
                break;
            }
        }
    }

}
