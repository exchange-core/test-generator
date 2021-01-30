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
import exchange.core2.benchmarks.generator.util.ExecutionTime;
import exchange.core2.benchmarks.generator.util.RandomUtils;
import exchange.core2.orderbook.util.BufferWriter;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.LongConsumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;


public final class MultiSymbolOrdersGenerator {

    public static final UnaryOperator<Integer> UID_PLAIN_MAPPER = i -> i + 1;

    private static final Logger log = LoggerFactory.getLogger(MultiSymbolOrdersGenerator.class);

    // TODO allow limiting max volume
    // TODO allow limiting number of opened positions (currently it just grows)
    // TODO use longs for prices (optionally)

    public static MultiSymbolGenResult generateMultipleSymbols(final OrdersGeneratorConfig config) {

        final List<GeneratorSymbolSpec> coreSymbolSpecifications = config.getCoreSymbolSpecifications();
        final int totalTransactionsNumber = config.getTotalTransactionsNumber();
        final List<BitSet> usersAccounts = config.getUsersAccounts();
        final int targetOrderBookOrdersTotal = config.getTargetOrderBookOrdersTotal();

        final int randomSeed = config.getSeed();
        final RandomGenerator rand = new JDKRandomGenerator(randomSeed);

        final Map<Integer, GenResult> genResultsMap = new HashMap<>();

        try (ExecutionTime ignore = new ExecutionTime(t -> log.debug("All test commands generated in {}", t))) {

            final double[] distribution = RandomUtils.weightedDistribution(coreSymbolSpecifications.size(), rand);
            int quotaLeft = totalTransactionsNumber;
            final Map<Integer, CompletableFuture<GenResult>> futures = new HashMap<>();

            final LongConsumer sharedProgressLogger = createAsyncProgressLogger(totalTransactionsNumber + targetOrderBookOrdersTotal);

            for (int i = coreSymbolSpecifications.size() - 1; i >= 0; i--) {
                final GeneratorSymbolSpec spec = coreSymbolSpecifications.get(i);
                final int orderBookSizeTarget = (int) (targetOrderBookOrdersTotal * distribution[i] + 0.5);
                final int commandsNum = (i != 0) ? (int) (totalTransactionsNumber * distribution[i] + 0.5) : Math.max(quotaLeft, 1);

                quotaLeft -= commandsNum;

//                log.debug("{}. Generating symbol {} : commands={} orderBookSizeTarget={} (quotaLeft={})", i, spec.symbolId, commandsNum, orderBookSizeTarget, quotaLeft);
                futures.put(spec.getSymbolId(), CompletableFuture.supplyAsync(() -> {
                    final int[] uidsAvailableForSymbol = ClientsCurrencyAccountsGenerator.createClientsListForSymbol(usersAccounts, spec, commandsNum);
                    final int numUsers = uidsAvailableForSymbol.length;
                    final UnaryOperator<Integer> uidMapper = idx -> uidsAvailableForSymbol[idx];

                    return SingleBookOrderGenerator.generateCommands(
                            commandsNum,
                            orderBookSizeTarget,
                            numUsers,
                            uidMapper,
                            spec,
                            false,
                            config.isAvalancheIOC(),
                            sharedProgressLogger,
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

        final CompletableFuture<BufferWriter> mergedCommandsFill = CompletableFuture.supplyAsync(
                () -> RandomCollectionsMerger.mergeCommands(
                        genResultsMap,
                        GenResult::getCommandsFill,
                        new JDKRandomGenerator(randomSeed)));

        // initiate merging process for benchmark commands part only when pre-fill commands are completed)
        final CompletableFuture<BufferWriter> mergedCommandsBenchmark = mergedCommandsFill.thenApplyAsync(ignore -> {
            log.debug("Merging {} BENCHMARK commands for {} symbols...",
                    genResultsMap.values().stream().mapToInt(GenResult::getNumCommandsBenchmark).sum(),
                    genResultsMap.size());

            return RandomCollectionsMerger.mergeCommands(
                    genResultsMap,
                    GenResult::getCommandsBenchmark,
                    new JDKRandomGenerator(randomSeed));
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


    public static LongConsumer createAsyncProgressLogger(int totalTransactionsNumber) {
        final long progressLogInterval = 5_000_000_000L; // 5 sec
        final AtomicLong nextUpdateTime = new AtomicLong(System.nanoTime() + progressLogInterval);
        final LongAdder progress = new LongAdder();
        return transactions -> {
            progress.add(transactions);
            final long whenLogNext = nextUpdateTime.get();
            final long timeNow = System.nanoTime();
            if (timeNow > whenLogNext) {
                if (nextUpdateTime.compareAndSet(whenLogNext, timeNow + progressLogInterval)) {
                    // whichever thread won - it should print progress
                    final long done = progress.sum();
                    log.debug(String.format("Generating commands progress: %.01f%% done (%d of %d)...",
                            done * 100.0 / totalTransactionsNumber, done, totalTransactionsNumber));
                }
            }
        };
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