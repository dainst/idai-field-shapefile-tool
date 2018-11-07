package org.dainst.idaifield;

import org.apache.commons.io.FileUtils;
import org.dainst.idaifield.model.GeometryType;
import org.dainst.idaifield.model.Resource;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;


/**
 * @author Thomas Kleinke
 */
public class ShapefileTool {

    public static void main(String[] arguments) {

        if (arguments.length != 3 || !arguments[1].contains(File.separator)) {
            System.err.println("java -jar shapefile-tool.jar PROJECT_NAME OUTPUT_FILE_PATH TEMP_FOLDER_PATH");
            return;
        }

        String projectName = arguments[0];
        String outputFolderPath = arguments[1].substring(0, arguments[1].lastIndexOf(File.separator));
        String outputFileName = arguments[1]
                .substring(arguments[1].lastIndexOf(File.separator), arguments[1].lastIndexOf('.'));
        String tempFolderPath = arguments[2];

        File shapefileFolder = createShapefileFolder(tempFolderPath, outputFileName);
        if (shapefileFolder == null) {
            System.err.println("Failed to create shapefile folder");
            return;
        }

        try {
            Map<GeometryType, List<Resource>> resources = ResourceProvider.getResources(projectName);
            ShapefileWriter.writeShapefile(shapefileFolder, resources);
            ZipArchiveBuilder.buildZipArchive(shapefileFolder, outputFolderPath);
        } catch(Exception e) {
            System.err.println(e);
        } finally {
            try {
                FileUtils.deleteDirectory(shapefileFolder);
            } catch(IOException e) {
                System.err.println(e);
            }
        }
    }


    private static File createShapefileFolder(String pathToTempFolder, String outputFileName) {

        File tempFolder = new File(pathToTempFolder + File.separator + outputFileName);
        if (!tempFolder.exists()) {
            if (!tempFolder.mkdirs()) return null;
        }

        System.out.println(tempFolder.getAbsolutePath());

        return tempFolder;
    }
}
