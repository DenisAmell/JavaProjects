package ru.mai.lessons.rpks.impl;

import ru.mai.lessons.rpks.IFileReader;
import ru.mai.lessons.rpks.exception.FilenameShouldNotBeEmptyException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class FileReader implements IFileReader {
    @Override
    public List<String> loadContent(String filePath) throws FilenameShouldNotBeEmptyException {

        if (filePath == null) {
            throw new FilenameShouldNotBeEmptyException("Invalid file path");
        }

        List<String> list = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new java.io.FileReader(Paths.get(getPathFile(filePath)).toString()));
            String line;
            while ((line = reader.readLine()) != null) {
                list.add(line);
            }
            System.out.println(list);
        } catch (URISyntaxException e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            throw new FilenameShouldNotBeEmptyException("Wrong filename");
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }


        return list;
    }

    private URI getPathFile(String fileName) throws URISyntaxException, FileNotFoundException {
        ClassLoader classLoader = getClass().getClassLoader();
        URL inputConfig = classLoader.getResource(fileName);

        if (inputConfig == null) {
            throw new FileNotFoundException("file " + fileName + " not found!");
        }
        return inputConfig.toURI();

    }
}