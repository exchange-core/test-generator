/*
 * Copyright 2018-2021 Maksim Zheravin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package exchange.core2.benchmarks.generator.orders;

import exchange.core2.benchmarks.generator.GeneratorSymbolSpec;
import exchange.core2.benchmarks.generator.clients.ClientsCurrencyAccountsGenerator;
import exchange.core2.benchmarks.generator.util.AsyncProgressLogger;
import exchange.core2.benchmarks.generator.util.ExecutionTime;
import exchange.core2.orderbook.util.BufferReader;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.LongConsumer;
import java.util.stream.Collectors;

public final class MultiSymbolOrdersGenerator {

    private static final Logger log = LoggerFactory.getLogger(MultiSymbolOrdersGenerator.class);

    // TODO allow limiting max volume
    // TODO allow limiting number of opened positions (currently it just grows)
    // TODO use longs for prices (optionally)

    public static MultiSymbolGenResult generateMultipleSymbols(
            final List<Pair<GeneratorSymbolSpec, Double>> symbolSpecs,
            final int totalTransactionsNumber,
            final List<BitSet> usersAccounts,
            final int targetOrderBookOrdersTotal,
            final int randomSeed,
            final boolean avalancheIOC) {

        final Map<Integer, GenResult> genResultsMap = new HashMap<>();

        try (ExecutionTime ignore = new ExecutionTime(t -> log.debug("All test commands generated in {}", t))) {

            if (Math.abs(symbolSpecs.stream().mapToDouble(Pair::getSecond).sum() - 1.0) > 0.000001) {
                throw new IllegalArgumentException("Symbol spec weights should be normalized");
            }

            final double linearWeightK = 1.0 / symbolSpecs.size();

            int quotaLeft = totalTransactionsNumber;
            int orderIdShift = 1;
            final Map<Integer, CompletableFuture<GenResult>> futures = new HashMap<>();

            final LongConsumer sharedProgressLogger = AsyncProgressLogger.createLoggingConsumer(
                    totalTransactionsNumber + targetOrderBookOrdersTotal,
                    message -> log.debug("Generating commands progress: {} ...", message),
                    5);

            for (int i = symbolSpecs.size() - 1; i >= 0; i--) {

                final Pair<GeneratorSymbolSpec, Double> wspec = symbolSpecs.get(i);
                final double weight = wspec.getSecond();
                final int commandsNum = (i != 0) ? (int) Math.round(totalTransactionsNumber * weight) : Math.max(quotaLeft, 1);

                // linearizing order book GTC orders distribution a bit (simulate market makers)
                final int orderBookSizeTarget = (int) Math.round(targetOrderBookOrdersTotal * (weight + linearWeightK) * 0.5);

                quotaLeft -= commandsNum;

                // maintaining unique orderId
                final int orderIdCounter = orderIdShift;
                orderIdShift += (commandsNum + orderBookSizeTarget);

                final GeneratorSymbolSpec spec = wspec.getFirst();

                //log.debug("{}. Generating symbol {} : commands={} orderBookSizeTarget={} (quotaLeft={})", i, spec.getSymbolId(), commandsNum, orderBookSizeTarget, quotaLeft);

                //log.debug("{}. {} b={} q={}", i, spec.getSymbolId(), spec.getBaseCurrency(), spec.getQuoteCurrency());

                futures.put(spec.getSymbolId(), CompletableFuture.supplyAsync(() -> {

                    // only some clients can trade specific symbols
                    final int[] uidsAvailableForSymbol = ClientsCurrencyAccountsGenerator.createClientsListForSymbol(
                            usersAccounts,
                            spec,
                            commandsNum,
                            randomSeed);

                    if (uidsAvailableForSymbol.length < 1) {
                        throw new IllegalArgumentException();
                    }

                    return SingleBookOrderGenerator.generateCommands(
                            commandsNum,
                            orderBookSizeTarget,
                            uidsAvailableForSymbol.length,
                            idx -> uidsAvailableForSymbol[idx],
                            spec,
                            false,
                            avalancheIOC,
                            sharedProgressLogger,
                            orderIdCounter,
                            randomSeed);
                }));
            }

            futures.forEach((symbol, future) -> {
                try {
                    genResultsMap.put(symbol, future.get());
                } catch (InterruptedException | ExecutionException ex) {
                    throw new IllegalStateException("Exception while generating commands for symbol " + symbol, ex);
                }
            });
        }

        log.debug("Merging {} PREFILL commands for {} symbols...",
                genResultsMap.values().stream().mapToInt(GenResult::getNumCommandsFill).sum(),
                genResultsMap.size());

        final CompletableFuture<BufferReader> mergedCommandsFill = CompletableFuture.supplyAsync(
                () -> RandomCollectionsMerger.mergeCommands(
                        genResultsMap,
                        GenResult::getCommandsFill,
                        new JDKRandomGenerator(randomSeed))
                        .toReader());

        // initiate merging process for benchmark commands part only when pre-fill commands are completed)
        final CompletableFuture<BufferReader> mergedCommandsBenchmark = mergedCommandsFill.thenApplyAsync(ignore -> {
            log.debug("Merging {} BENCHMARK commands for {} symbols...",
                    genResultsMap.values().stream().mapToInt(GenResult::getNumCommandsBenchmark).sum(),
                    genResultsMap.size());

            return RandomCollectionsMerger.mergeCommands(
                    genResultsMap,
                    GenResult::getCommandsBenchmark,
                    new JDKRandomGenerator(randomSeed))
                    .toReader();
        });


        final int benchmarkCmdSize = genResultsMap.values().stream()
                .mapToInt(GenResult::getNumCommandsBenchmark)
                .sum();

        final Map<Integer, Integer> bookHashes = genResultsMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getFinalOrderBookHash()));

        return new MultiSymbolGenResult(
                bookHashes,
                mergedCommandsFill,
                mergedCommandsBenchmark,
                benchmarkCmdSize);
    }

//
//    // TODO create class and agregate from single orderbook
//    private static void printStatistics(final byte[] allCommands) {
//        int counterPlaceIOC = 0;
//        int counterPlaceGTC = 0;
//        int counterPlaceFOKB = 0;
//        int counterCancel = 0;
//        int counterMove = 0;
//        int counterReduce = 0;
//        final IntIntHashMap symbolCounters = new IntIntHashMap();
//
//        for (byte[] cmd : allCommands) {
//            switch (cmd.command) {
//                case MOVE_ORDER:
//                    counterMove++;
//                    break;
//
//                case CANCEL_ORDER:
//                    counterCancel++;
//                    break;
//
//                case REDUCE_ORDER:
//                    counterReduce++;
//                    break;
//
//                case PLACE_ORDER:
//                    if (cmd.orderType == OrderType.IOC) {
//                        counterPlaceIOC++;
//                    } else if (cmd.orderType == OrderType.GTC) {
//                        counterPlaceGTC++;
//                    } else if (cmd.orderType == OrderType.FOK_BUDGET) {
//                        counterPlaceFOKB++;
//                    }
//                    break;
//            }
//            symbolCounters.addToValue(cmd.symbol, 1);
//        }
//
//        final int commandsListSize = allCommands.size();
//        final IntSummaryStatistics symbolStat = symbolCounters.summaryStatistics();
//
//        final String commandsGtc = String.format("%.2f%%", (float) counterPlaceGTC / (float) commandsListSize * 100.0f);
//        final String commandsIoc = String.format("%.2f%%", (float) counterPlaceIOC / (float) commandsListSize * 100.0f);
//        final String commandsFokb = String.format("%.2f%%", (float) counterPlaceFOKB / (float) commandsListSize * 100.0f);
//        final String commandsCancel = String.format("%.2f%%", (float) counterCancel / (float) commandsListSize * 100.0f);
//        final String commandsMove = String.format("%.2f%%", (float) counterMove / (float) commandsListSize * 100.0f);
//        final String commandsReduce = String.format("%.2f%%", (float) counterReduce / (float) commandsListSize * 100.0f);
//        log.info("GTC:{} IOC:{} FOKB:{} cancel:{} move:{} reduce:{}", commandsGtc, commandsIoc, commandsFokb, commandsCancel, commandsMove, commandsReduce);
//
//        final String cpsMax = String.format("%d (%.2f%%)", symbolStat.getMax(), symbolStat.getMax() * 100.0f / commandsListSize);
//        final String cpsAvg = String.format("%d (%.2f%%)", (int) symbolStat.getAverage(), symbolStat.getAverage() * 100.0f / commandsListSize);
//        final String cpsMin = String.format("%d (%.2f%%)", symbolStat.getMin(), symbolStat.getMin() * 100.0f / commandsListSize);
//        log.info("commands per symbol: max:{}; avg:{}; min:{}", cpsMax, cpsAvg, cpsMin);
//    }

}
