package ru.mai.lessons.rpks.impl;

import ru.mai.lessons.rpks.IConfigReader;
import ru.mai.lessons.rpks.exception.FilenameShouldNotBeEmptyException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigReader implements IConfigReader {

    @Override
    public String loadConfig(String configPath) throws FilenameShouldNotBeEmptyException {


        if (configPath == null) {
            throw new FilenameShouldNotBeEmptyException("Wrong Filename");
        }

        String stringConfig = "";

        try {
            stringConfig = new String(Files.readAllBytes(Paths.get(getPathConfigFile(configPath))));
        } catch (URISyntaxException ex) {
            System.err.println(ex.getMessage());
        } catch (IOException e) {
            throw new FilenameShouldNotBeEmptyException("Wrong filename");
        }

        return stringConfig;
    }

    private URI getPathConfigFile(String fileName) throws URISyntaxException, FileNotFoundException {
        ClassLoader classLoader = getClass().getClassLoader();
        URL inputConfig = classLoader.getResource(fileName);

        if (inputConfig == null) {
            throw new FileNotFoundException("file " + fileName + " not found!");
        }
        return inputConfig.toURI();

    }
}