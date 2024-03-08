package ru.mai.lessons.rpks.impl;

import ru.mai.lessons.rpks.IMoneyExchange;
import ru.mai.lessons.rpks.exception.ExchangeIsImpossibleException;

import java.util.*;
import java.util.Map;
import java.util.TreeMap;

public class MoneyExchange implements IMoneyExchange {
    public String exchange(Integer sum, String coinDenomination) throws ExchangeIsImpossibleException {

        if(coinDenomination.isEmpty()) throw new ExchangeIsImpossibleException("Размен монет невозможен");

        int[] coins = Arrays.stream(coinDenomination.split(", ")).mapToInt(Integer::parseInt).toArray();

        if(!checkToNull(coins)) throw new ExchangeIsImpossibleException("Размен монет невозможен");

        Map<Integer, Integer> coinsMap = coinChangeRecursive(sum, coins, 0);

        if (coinsMap == null) throw new ExchangeIsImpossibleException("Размен монет невозможен");

        var result = new StringBuilder();

        for (var item : coinsMap.entrySet()) {
            if (item.getValue() != 0) {
                result.append(item.getKey()).append("[").append(item.getValue()).append("]").append(", ");
            }
        }

        result.deleteCharAt(result.length() - 1)
                .deleteCharAt(result.length() - 1);

        return result.toString();

    }

    private Map<Integer, Integer> coinChangeRecursive(Integer sum, int[] coins, int index)  {

        if (sum == 0) {
            return new TreeMap<>((a, b) -> b.compareTo(a));
        }

        if (index >= coins.length || sum < 0) {
            return null;
        }

        int coin = coins[index];
        if (coin <= 0) {
            return coinChangeRecursive(sum, coins, index + 1);
        }
        int maxCount = sum / coin;
        Map<Integer, Integer> minChange = null;

        for (int count = maxCount; count >= 0; count--) {
            int remainingAmount = sum - count * coin;
            Map<Integer, Integer> subChange = coinChangeRecursive(remainingAmount, coins, index + 1);
            if (subChange != null) {
                subChange.put(coin, count);
                if (minChange == null || totalCoins(minChange) > totalCoins(subChange)) {
                    minChange = subChange;
                }
            }
    }
        return minChange;
    }

    private int totalCoins(Map<Integer, Integer> change) {
        int total = 0;
        for (int count : change.values()) {
            total += count;
        }
        return total;
    }

    private boolean checkToNull(int[] arr) {
        boolean isPossible = false;
        for (int item : arr) {
            if (item > 0) {
                isPossible = true;
            }
        }
        return isPossible;
    }

}
