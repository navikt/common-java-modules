package no.nav.apiapp.util;

import no.nav.apiapp.ApiApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;

/**
 * Helper class for locating the path to the war folder where the application resides, in other words to check
 * if the application is run locally on a developer workstation or in a docker container.
 */
public class WarFolderFinderUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(WarFolderFinderUtil.class);

    /**
     * For application instances started from a code repo the path to the war folder under the sources folder is
     * returned, in all other cases the default Docker application path is returned.
     *
     * @param apiAppClass the main application context for the Api-App
     * @return path to the war folder
     */
    public static File findPath(Class<? extends ApiApplication> apiAppClass) {
        Optional<File> sourcesPath = locateSourcesPath(apiAppClass);
        if (isRunningLocally(sourcesPath)) {
            return addWebAppFolder(sourcesPath.get());
        } else {
            LOGGER.debug("Falling back to use the default Docker war folder");
            return new File("/app");
        }
    }

    private static Optional<File> locateSourcesPath(Class<? extends ApiApplication> apiAppClass) {
        Optional<File> baseDir = locateBaseDir(apiAppClass);
        if (baseDir.isPresent()) {
            File sourcesPath = new File(baseDir.get(), "src/main");
            return Optional.of(sourcesPath);
        } else {
            return Optional.empty();
        }
    }

    private static Optional<File> locateBaseDir(Class<? extends ApiApplication> apiAppClass) {
        try {
            File classesDir = new File(apiAppClass.getResource("/").toURI());
            File baseDir = new File(classesDir, "../../").getCanonicalFile();
            return Optional.of(baseDir);
        } catch (Exception e) {
            LOGGER.warn("Sources path not found.", e);
            return Optional.empty();
        }
    }

    private static boolean isRunningLocally(Optional<File> sourcesPath) {
        return sourcesPath.isPresent() && sourcesPath.get().exists();
    }

    private static File addWebAppFolder(File sourcesPath) {
        File webAppPath = new File(sourcesPath, "webapp");
        webAppPath.mkdir();
        return webAppPath;
    }

}
