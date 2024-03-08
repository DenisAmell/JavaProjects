package ru.mai.lessons.rpks.impl;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import ru.mai.lessons.rpks.ILineFinder;
import ru.mai.lessons.rpks.exception.LineCountShouldBePositiveException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;


public class LineFinder implements ILineFinder {

    private static final int SIZE_BLOCK_IN_BYTE = 1024 * 1024;

    @Override
    public void find(String inputFilename, String outputFilename, String keyWord, int lineCount) throws LineCountShouldBePositiveException {

        if (lineCount < 0) {
            throw new LineCountShouldBePositiveException("Input line count error");
        }

        if (keyWord == null) {
            System.err.println("Key word is empty");
            throw new RuntimeException("Key word is empty");
        }


        try {
            Set<Long> lineSet = readFileInChunks(inputFilename, keyWord);
            ExecutorService service = Executors.newFixedThreadPool(1);
            try (RandomAccessFile outputFile = new RandomAccessFile(getPath(outputFilename), "rw")) {
                service.submit(() -> {
                    try {
                        outputTask(inputFilename, outputFile, lineSet, lineCount);
                    } catch (IOException e) {
                        System.err.println(e.getMessage());
                        throw new RuntimeException(e);
                    }
                });

                service.shutdown();
                try {
                    if (!service.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                        service.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    service.shutdownNow();
                }
            }
        } catch (IOException | ExecutionException | InterruptedException e) {
            System.err.println(e.getMessage());
        } catch (URISyntaxException e) {
            System.out.println(e.getMessage());
        }
    }


    private Set<Long> readFileInChunks(String inputFilename, String keyWord) throws IOException, URISyntaxException, ExecutionException, InterruptedException {

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<Set<Long>>> futures = new ArrayList<>();


        try (RandomAccessFile fileInputStream = new RandomAccessFile(getPath(inputFilename), "r")) {
            int bytesRead = 0;
            while ((bytesRead < fileInputStream.length())) {
                int finalBytesToRead = bytesRead;
                futures.add(executorService.submit(() -> threadReadTask(fileInputStream, keyWord, finalBytesToRead)));
                bytesRead += SIZE_BLOCK_IN_BYTE;
            }

            executorService.shutdown();

            try {
                if (!executorService.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }


            Set<Long> lines = new TreeSet<>();
            for (var item : futures) {
                if (item.isDone()) {
                    lines.addAll(item.get());
                }
            }


            return lines;
        }
    }


    private Set<Long> threadReadTask(RandomAccessFile inputFile, String keyWord, int position) throws IOException {

        List<Long> pointers = new ArrayList<>();
        try {
            int bytesRead = 0;
            byte byteTmp = 0;
            inputFile.seek(position);
            while (bytesRead < SIZE_BLOCK_IN_BYTE && inputFile.getFilePointer() < inputFile.length()) {
                Pair<String, Integer> token = readToken(inputFile);

                if (token.getLeft().toLowerCase().contains(keyWord.toLowerCase())) {
                    pointers.add(inputFile.getFilePointer() - token.getRight());
                    if (!token.getLeft().contains(System.lineSeparator())) {

                        while (byteTmp != '\n' && inputFile.getFilePointer() < inputFile.length()) {
                            byteTmp = inputFile.readByte();
                            bytesRead++;
                        }
                    }
                }

                bytesRead += token.getRight();
            }

            Set<Long> lines = new TreeSet<>();
            for (Long filePointer : pointers) {
                byte symbol = 0;
                int offset = 0;

                while (symbol != '\n' && offset < filePointer) {
                    inputFile.seek(filePointer - ++offset);
                    symbol = inputFile.readByte();
                }

                lines.add(filePointer - offset + (offset < filePointer ? 1 : 0));
            }

            return lines;

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    private void outputTask(String inputFilename, RandomAccessFile outputFile, Set<Long> lines, int lineCount) throws IOException {

        try (RandomAccessFile inputFile = new RandomAccessFile(getPath(inputFilename), "r")) {

            Iterator<Long> iterLine = lines.iterator();
           while(iterLine.hasNext()) {
                var item = iterLine.next();
                int index = 0;
                long offset = 0;
                while (index != lineCount + 1 && offset < item) {
                    byte byteFile = 0;
                    while (byteFile != '\n' && offset < item) {

                        inputFile.seek(item - ++offset);
                        byteFile = inputFile.readByte();
                    }
                    index++;
                }

                offset = offset < item ? offset - 1 : item;
                index = item == 0 ? index + 1 : index;
                inputFile.seek(item - offset);
                boolean last = !iterLine.hasNext();
                outputInFile(inputFile, outputFile, index, lineCount, last);
            }
        }
    }


    private void outputInFile(RandomAccessFile inputFile, RandomAccessFile outputFile, int lineNumber, int lineCount, boolean last)  throws IOException{

        for (int i = 0; i < lineCount + lineNumber && inputFile.getFilePointer() < inputFile.length(); i++) {
            byte symbol = 0;

            while (symbol != '\n' && inputFile.getFilePointer() < inputFile.length()) {
                symbol = inputFile.readByte();

                if (!(i == lineCount + lineNumber - 1 && symbol == '\n' && last)) {
                    outputFile.writeByte(symbol);
                }
            }
        }
    }

    private Pair<String, Integer> readToken(RandomAccessFile inputFile) throws IOException {
        List<Byte> listBytes = new ArrayList<>();

        int bufferSize = 0;
        byte byteRead = 0;
        while (byteRead != '\n' && inputFile.getFilePointer() < inputFile.length()) {
            byteRead = inputFile.readByte();
            listBytes.add(byteRead);
            bufferSize++;
        }
        return Pair.of(new String(ArrayUtils.toPrimitive(listBytes.toArray(new Byte[0])), StandardCharsets.UTF_8), bufferSize);
    }

    private String getPath(String filename) {
        return Objects.requireNonNull(getClass().getClassLoader().getResource(".")).getPath() + filename;
    }
}