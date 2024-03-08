package ru.mai.lessons.rpks;

import ru.mai.lessons.rpks.exception.LineCountShouldBePositiveException;

import java.io.IOException;
import java.net.URISyntaxException;

public interface ILineFinder {
  public void find(String inputFilename, String outputFilename, String keyWord,
                   int lineCount) throws LineCountShouldBePositiveException; // запускает поиск
}
