package ru.mai.lessons.rpks.impl;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import ru.mai.lessons.rpks.ILogAnalyzer;
import ru.mai.lessons.rpks.exception.WrongFilenameException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;


public class LogAnalyzer implements ILogAnalyzer {

    private static final int SIZE_BLOCK_IN_BYTE = 1024 * 1024;

    private Integer getTimeInSeconds(String timeInString) {
        StringBuilder timeStr = new StringBuilder();
        int result = 0;


        if (timeInString.contains("sec")) {
            timeStr.append(timeInString, 0, timeInString.indexOf("sec") - 1);
            result = Integer.parseInt(timeStr.toString());
        }
        if (timeInString.contains("min")) {
            timeStr.append(timeInString, 0, timeInString.indexOf("min") - 1);
            result = Integer.parseInt(timeStr.toString()) * 60;
        }
        if (timeInString.contains("hour")) {
            timeStr.append(timeInString, 0, timeInString.indexOf("hour") - 1);
            result = Integer.parseInt(timeStr.toString()) * 3600;
        }

        return result;
    }

    @Override
    public List<Integer> analyze(String filename, String deviation) throws WrongFilenameException {

        if (filename.isEmpty()) {
            System.err.println("File is empty or him not found");
            throw new WrongFilenameException("filename is empty");
        }

        try {
            List<Integer> invalidId = new ArrayList<>();
            Map<Integer, Long> complitedLogsMap = readFileInChunks(filename);
            int medianTime = calculateAverageDuration(complitedLogsMap);

            ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            for (var item : complitedLogsMap.entrySet()) {
                executorService.submit(() -> {
                    if (item.getValue() - medianTime > getTimeInSeconds(deviation)) {
                        invalidId.add(item.getKey());
                    }
                });
            }

            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }

            invalidId.sort(Comparator.naturalOrder());

            return invalidId;
        } catch (ExecutionException ex) {
            System.err.println("ExecutionException caught");
            throw new WrongFilenameException("ExecutionException caught");
        } catch (InterruptedException | IOException | URISyntaxException ex) {
            System.err.println("InterruptedException caught");
            throw new WrongFilenameException("InterruptedException caught");
        }
    }

    private Map<Integer, Long> readFileInChunks(String inputFilename) throws IOException, URISyntaxException, ExecutionException, InterruptedException {

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<List<LogEntry>>> futures = new ArrayList<>();


        try (RandomAccessFile fileInputStream = new RandomAccessFile(Paths.get(getURIResourcesFile(inputFilename)).toString(), "r")) {
            int bytesRead = 0;
            while ((bytesRead < fileInputStream.length())) {
                int finalBytesToRead = bytesRead;
                futures.add(executorService.submit(() -> threadReadTask(fileInputStream, finalBytesToRead)));
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

            Map<Integer, Long> completedRequests = new HashMap<>();
            Map<Integer, Pair<Boolean, LocalDateTime>> notCompletedRequests = new HashMap<>();

            for (var item : futures) {
                if (item.isDone()) {
                    List<LogEntry> logEntries = item.get();

                    for (var entry : logEntries) {
                        int idLogs = entry.getId();
                        Boolean statusLogs = entry.getStatus();
                        LocalDateTime timeRequest = entry.getTimestamp();
                        if (!statusLogs) {
                            notCompletedRequests.put(idLogs, Pair.of(statusLogs, timeRequest));
                        } else {
                            if (notCompletedRequests.containsKey(idLogs)) {
                                completedRequests.put(idLogs, Math.abs(ChronoUnit.SECONDS.between(timeRequest, notCompletedRequests.get(idLogs).getRight())));
                                notCompletedRequests.remove(idLogs);

                            } else {
                                notCompletedRequests.put(idLogs, Pair.of(statusLogs, timeRequest));
                            }
                        }
                    }

                }
            }
            return completedRequests;
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

    private List<LogEntry> threadReadTask(RandomAccessFile inputFile, int position) {
        try {
            inputFile.seek(position);
            int bytesRead = 0;

            List<LogEntry> logEntries = new ArrayList<>();

            Pair<String, Integer> token = null;
            while (bytesRead < SIZE_BLOCK_IN_BYTE && inputFile.getFilePointer() < inputFile.length()) {
                token = readToken(inputFile);

                LogEntry entry = parseLogEntry(token.getLeft());

                if (entry != null) {
                    logEntries.add(entry);
                }
                bytesRead += token.getRight();
            }
            return logEntries;
        } catch (IOException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static LogEntry parseLogEntry(String line) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String[] parts = line.split(" – ");
        if (parts.length >= 3) {
            try {
                LocalDateTime timestamp = LocalDateTime.parse(parts[0], formatter);
                String message = parts[2];
                Boolean status = message.contains("RESULT") ? true : false;
                int id = getIdLogs(message);
                return new LogEntry(timestamp, message, status, id);
            } catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }
        return null;
    }

    private static int getIdLogs(String message) {
        String tmp = StringUtils.stripEnd(message, "\r\n");
        return Integer.parseInt(tmp.substring(tmp.indexOf("=") + 2));
    }

    private static int calculateAverageDuration(Map<Integer, Long> compiledLogs) {
        if (compiledLogs.isEmpty()) {
            return 0;
        }

        List<Integer> counting = new ArrayList<>();

        for (var item : compiledLogs.entrySet()) {
            addCounting(counting, item.getValue().intValue());
        }

        long countCompletedRequest = counting.stream().mapToInt(Integer::intValue).sum();
        long temp = 0;
        int index = 0;

        while (temp < countCompletedRequest / 2) {
            temp += counting.get(index++);
        }

        return index - 1;
    }

    private static void addCounting(List<Integer> listCounting, int timeExecution) {
        if (timeExecution >= listCounting.size()) {
            Integer[] temp = new Integer[timeExecution - listCounting.size() + 1];
            Arrays.fill(temp, 0);
            listCounting.addAll(List.of(temp));
        }

        listCounting.set(timeExecution, listCounting.get(timeExecution) + 1);
    }

    private static List<Integer> findAnomalousEntries(List<LogEntry> logEntries, Duration averageDuration, Duration deviation) {
        List<Integer> anomalousEntries = new ArrayList<>();

        Map<Integer, Long> completedRequests = new HashMap<>();
        Map<Integer, Pair<Boolean, LocalDateTime>> notCompletedRequests = new HashMap<>();
        for (var item : logEntries) {
            int idLogs = item.getId();
            Boolean statusLogs = item.getStatus();
            LocalDateTime timeRequest = item.getTimestamp();
            if (!statusLogs) {
                notCompletedRequests.put(idLogs, Pair.of(statusLogs, timeRequest));
            } else {
                if (notCompletedRequests.containsKey(idLogs)) {
                    completedRequests.put(idLogs, Math.abs(ChronoUnit.SECONDS.between(timeRequest, notCompletedRequests.get(idLogs).getRight())));
                    notCompletedRequests.remove(idLogs);
                } else {
                    notCompletedRequests.put(idLogs, Pair.of(statusLogs, timeRequest));
                }
            }
        }

        for (var item : completedRequests.entrySet()) {
            if (Math.abs(item.getValue() - averageDuration.toSeconds()) >= deviation.toSeconds()) {
                anomalousEntries.add(item.getKey());
            }
        }

        return anomalousEntries;
    }

    private static class LogEntry {
        private LocalDateTime timestamp;
        private String message;
        private Boolean status;

        private int id;

        public LogEntry(LocalDateTime timestamp, String message, Boolean status, int id) {
            this.timestamp = timestamp;
            this.message = message;
            this.status = status;
            this.id = id;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return timestamp.toString() + " - " + message;
        }

        public Boolean getStatus() {
            return status;
        }

        public int getId() {
            return id;
        }
    }

    private URI getURIResourcesFile(String fileName) throws URISyntaxException, FileNotFoundException {
        ClassLoader classLoader = getClass().getClassLoader();
        URL inputConfig = classLoader.getResource(fileName);

        if (inputConfig == null) {
            throw new FileNotFoundException("file " + fileName + " not found!");
        }
        return inputConfig.toURI();

    }
}