package no.nav.common.test.ssl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;

final class PrivateTestFiles {

    private PrivateTestFiles() {}

    static File create(String prefix, String suffix) throws IOException {
        Path tempRoot = Path.of(System.getProperty("java.io.tmpdir"));
        boolean posix = Files.getFileStore(tempRoot).supportsFileAttributeView("posix");
        FileAttribute<?>[] directoryAttributes = posix
                ? new FileAttribute<?>[] {
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"))
                }
                : new FileAttribute<?>[0];
        FileAttribute<?>[] fileAttributes = posix
                ? new FileAttribute<?>[] {
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))
                }
                : new FileAttribute<?>[0];

        Path directory = Files.createTempDirectory(tempRoot, "nav-test-ssl-", directoryAttributes);
        directory.toFile().deleteOnExit();
        Path file = Files.createTempFile(directory, prefix, suffix, fileAttributes);
        file.toFile().deleteOnExit();
        return file.toFile();
    }
}
