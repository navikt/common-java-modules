package no.nav.apiapp.util;

import java.util.Scanner;

public class FileUtils {

    public String readContentFromFile(String fileName) {
        return new Scanner(getClass().getClassLoader().getResourceAsStream(fileName), "UTF-8").useDelimiter("\\A").next();
    }
}
