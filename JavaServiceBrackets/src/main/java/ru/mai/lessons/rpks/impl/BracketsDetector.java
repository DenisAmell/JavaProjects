package ru.mai.lessons.rpks.impl;

import org.javatuples.Triplet;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import ru.mai.lessons.rpks.IBracketsDetector;
import ru.mai.lessons.rpks.result.ErrorLocationPoint;

import java.util.*;

public class BracketsDetector implements IBracketsDetector {
    @Override
    public List<ErrorLocationPoint> check(String config, List<String> content) {
        List<ErrorLocationPoint> errorsList = new ArrayList<>();
        Deque<Triplet<Character, Integer, Integer>> stack = new ArrayDeque<>();
        try {
            Map<Character, Character> jsonMap = getMapJson(config);

            int lineNumber = 0;
            int symNumber;
            for (String line : content) {
                symNumber = 0;
                lineNumber++;
                int indexErrorForIdenticalBrackets = 0;
                int countIdenticalBrackets = 0;
                for (Character symbol : line.toCharArray()) {
                    symNumber++;
                    if (jsonMap.containsValue(symbol)) {
                        if (symbol == '|') {
                            countIdenticalBrackets++;
                            indexErrorForIdenticalBrackets = symNumber;
                        } else {
                            stack.push(new Triplet<>(symbol, lineNumber, symNumber));
                        }
                    } else if (jsonMap.containsKey(symbol)) {
                        if (stack.isEmpty() || stack.peek().getValue0() != jsonMap.get(symbol)) {
                            errorsList.add(new ErrorLocationPoint(lineNumber, symNumber));
                            continue;
                        }
                        stack.pop();
                    }
                }

                if ((countIdenticalBrackets & 1) == 1) {
                    errorsList.add(new ErrorLocationPoint(lineNumber, indexErrorForIdenticalBrackets));
                }

                while (!stack.isEmpty()) {
                    Triplet<Character, Integer, Integer> element = stack.pop();
                    errorsList.add(new ErrorLocationPoint(element.getValue1(), element.getValue2()));
                }

            }
        } catch (ParseException e) {
            System.err.println(e.getMessage());
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return errorsList;
    }

    private Map<Character, Character> getMapJson(String json) throws ParseException {

        Map<Character, Character> brackets = new HashMap<>();

        try {

            JSONArray jsonArray = (JSONArray) ((JSONObject) new JSONParser().parse(json)).get("bracket");
            for (Object bracket : jsonArray) {
                JSONObject jsonBracket = (JSONObject) bracket;
                brackets.put(((String) jsonBracket.get("right")).charAt(0),
                        ((String) jsonBracket.get("left")).charAt(0));
            }
        } catch (ParseException e) {
            System.err.println(e.getMessage());
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return brackets;
    }
}
