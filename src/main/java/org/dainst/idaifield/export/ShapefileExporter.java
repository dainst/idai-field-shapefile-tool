package org.dainst.idaifield.export;

import org.apache.commons.io.FileUtils;
import org.dainst.idaifield.model.GeometryType;
import org.dainst.idaifield.model.Resource;

import java.io.File;
import java.util.List;
import java.util.Map;


/**
 * @author Thomas Kleinke
 */
public class ShapefileExporter {

    public static void export(String projectName, String outputFilePath, String tempFolderPath,
                              String operationId, String epsg) throws Exception {

        String outputFolderPath = outputFilePath.substring(0, outputFilePath.lastIndexOf(File.separator));
        String outputFileName = outputFilePath.substring(
                outputFilePath.lastIndexOf(File.separator), outputFilePath.lastIndexOf('.')
        );

        File shapefileFolder = createShapefileFolder(tempFolderPath, outputFileName);
        if (shapefileFolder == null) {
            System.err.println("Failed to create shapefile folder");
            return;
        }

        try {
            Map<GeometryType, List<Resource>> resources = ResourceProvider.getResources(projectName,
                    operationId);
            ShapefileWriter.write(shapefileFolder, resources, epsg);
            ZipArchiveBuilder.buildZipArchive(shapefileFolder, outputFolderPath);
        } finally {
            FileUtils.deleteDirectory(shapefileFolder);
        }
    }


    private static File createShapefileFolder(String pathToTempFolder, String outputFileName) {

        File tempFolder = new File(pathToTempFolder + File.separator + outputFileName);
        if (!tempFolder.exists()) {
            if (!tempFolder.mkdirs()) return null;
        }

        return tempFolder;
    }
}
