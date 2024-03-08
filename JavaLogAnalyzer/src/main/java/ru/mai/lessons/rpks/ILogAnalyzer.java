package ru.mai.lessons.rpks;

import ru.mai.lessons.rpks.exception.WrongFilenameException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public interface ILogAnalyzer {
  public List<Integer> analyze(String filename, String deviation) throws WrongFilenameException; // запускает проверку
}
