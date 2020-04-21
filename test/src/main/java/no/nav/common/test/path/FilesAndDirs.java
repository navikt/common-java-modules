package no.nav.common.test.path;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public final class FilesAndDirs {

    static {
        try {
            File classesDir = new File(FilesAndDirs.class.getResource("/").toURI());
            PROJECT_BASEDIR = new File(classesDir, "../../").getCanonicalFile();
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static final File PROJECT_BASEDIR;


    public static final File MAIN_DIR = new File(PROJECT_BASEDIR, "src/main");
    public static final File TEST_DIR = new File(PROJECT_BASEDIR, "src/test");


    public static final File RESOURCES = new File(MAIN_DIR, "resources");
    public static final File TEST_RESOURCES = new File(TEST_DIR, "resources");


    public static final File WEBAPP_SOURCE = new File(MAIN_DIR, "webapp");
    public static final File TEST_WEBAPP_SOURCE = new File(TEST_DIR, "webapp");


    public static final File BUILD_OUTPUT = new File(PROJECT_BASEDIR, "target");
    public static final File POM_XML = new File(PROJECT_BASEDIR, "pom.xml");


    private FilesAndDirs() { }
}
