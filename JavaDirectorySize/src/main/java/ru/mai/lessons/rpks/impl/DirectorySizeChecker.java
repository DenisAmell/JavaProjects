package ru.mai.lessons.rpks.impl;

import ru.mai.lessons.rpks.IDirectorySizeChecker;
import ru.mai.lessons.rpks.exception.DirectoryAccessException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DirectorySizeChecker implements IDirectorySizeChecker {
    @Override
    public String checkSize(String directoryName) throws DirectoryAccessException {

        Path path = Paths.get("src/test/resources/" + directoryName);

        long sizeDirectory = 0;

        if (!Files.isDirectory(path)) throw new DirectoryAccessException("Not found directory");

        try {
            Stream<Path> walk = Files.walk(path);
            sizeDirectory = walk.filter(Files::isRegularFile).mapToLong(
                    p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            log.info("Не получилось считать размер файла либо его нет");
                            return 0L;
                        }
                    }).sum();
        } catch (IOException e) {
            System.err.println("Ошибка при подсчёте размера директории");
        }

        return Long.toString(sizeDirectory) + " bytes";
    }
}
