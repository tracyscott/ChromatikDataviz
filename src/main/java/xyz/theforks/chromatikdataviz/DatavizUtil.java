package xyz.theforks.chromatikdataviz;

import heronarts.lx.LX;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class DatavizUtil {
    public static boolean resourceFilesCopied = false;

    static protected String getDatavizDir(LX lx) {
        return lx.getMediaPath() + File.separator + "dataviz" + File.separator;
    }

    static public void exportDatavizFiles(LX lx) {
        if (resourceFilesCopied) {
            return;
        }
        LX.log("Exporting default files for ChromatikDataviz");
        // Copy all the files from the resources/dataviz folder in the jar that this class
        // came from into the folder getDatavizDir()
        String datavizDir = getDatavizDir(lx);
        File datavizDirFile = new File(datavizDir);
        if (!datavizDirFile.exists()) {
            datavizDirFile.mkdirs();
        }
        // Inspect the directory contents of resources/dataviz in the jar file that this
        // class was loaded from.
        List<String> includedDataFiles = DatavizUtil.getIncludedDataFiles(DatavizUtil.class, "dataviz");
        for (String includedDataFile : includedDataFiles) {
            File dataFile = new File(datavizDir + includedDataFile);
            if (!dataFile.exists()) {
                LX.log("Exporting ChromatikDataviz file: " + includedDataFile);
                try {
                    DatavizUtil.copyResourceToFile(DatavizUtil.class, "dataviz/" + includedDataFile, dataFile);
                } catch (Exception e) {
                    LX.log("Error copying dataviz file: " + e.getMessage());
                }
            }
        }
        resourceFilesCopied = true;
    }

    static public List<String> getIncludedDataFiles(Class<?> clazz, String resourcePath) {
        return listResourceFiles(clazz, resourcePath);
    }

    static public List<String> listResourceFiles(Class<?> clazz, String resourcePath) {
        try {
            URL resourceUrl = clazz.getClassLoader().getResource(resourcePath);
            if (resourceUrl == null) {
                return Collections.emptyList();
            }

            if (resourceUrl.getProtocol().equals("jar")) {
                // Handle resources in JAR
                try (FileSystem fileSystem = FileSystems.newFileSystem(
                        resourceUrl.toURI(), Collections.<String, Object>emptyMap())) {
                    Path path = fileSystem.getPath(resourcePath);
                    return listFiles(path);
                }
            } else {
                // Handle resources in regular directory (useful for development)
                Path path = Path.of(resourceUrl.toURI());
                return listFiles(path);
            }
        } catch (URISyntaxException | IOException e) {
            LX.log("Error listing resource files: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    static private List<String> listFiles(Path path) throws IOException {
        List<String> fileList = new ArrayList<>();

        try (Stream<Path> walk = Files.walk(path, 1)) {
            walk.filter(Files::isRegularFile)
                    .forEach(filePath -> {
                        String fileName = filePath.getFileName().toString();
                        fileList.add(fileName);
                    });
        }

        return fileList;
    }

    /**
     * Copy a file from the resources path of a JAR to a file on disk.
     * @param gstClass Determines the class loader to use. Should be a class in our target package.
     * @param s The path to the file in the resources directory.
     * @param dataFile The file to copy the resource to.
     */
    public static void copyResourceToFile(Class<DatavizUtil> gstClass, String s, File dataFile) {
        // Copy a file from the resources path of a JAR to a file on disk
        try {
            URL resourceUrl = gstClass.getClassLoader().getResource(s);
            if (resourceUrl == null) {
                LX.log("Resource not found: " + s);
                return;
            }

            if (resourceUrl.getProtocol().equals("jar")) {
                // Handle resources in JAR
                try (FileSystem fileSystem = FileSystems.newFileSystem(
                        resourceUrl.toURI(), Collections.<String, Object>emptyMap())) {
                    Path path = fileSystem.getPath(s);
                    Files.copy(path, dataFile.toPath());
                }
            } else {
                // Handle resources in regular directory (useful for development)
                Path path = Path.of(resourceUrl.toURI());
                Files.copy(path, dataFile.toPath());
            }
        } catch (URISyntaxException | IOException e) {
            LX.log("Error copying resource file: " + e.getMessage());
        }
    }
}
