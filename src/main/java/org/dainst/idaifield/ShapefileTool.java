package org.dainst.idaifield;

import org.dainst.idaifield.export.ShapefileExporter;

import java.io.File;


/**
 * @author Thomas Kleinke
 */
public class ShapefileTool {

    public static void main(String[] arguments) {

        if (arguments.length != 3 || !arguments[1].contains(File.separator)) {
            System.err.println("java -jar shapefile-tool.jar PROJECT_NAME OUTPUT_FILE_PATH TEMP_FOLDER_PATH");
            return;
        }

        try {
            ShapefileExporter.export(arguments[0], arguments[1], arguments[2]);
        } catch(Exception e) {
            System.err.println(e);
        }
    }
}
