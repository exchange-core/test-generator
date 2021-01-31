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
package exchange.core2.benchmarks.generator.symbols;

import exchange.core2.benchmarks.generator.GeneratorSymbolSpec;
import exchange.core2.benchmarks.generator.util.RandomUtils;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class SymbolsGenerator {

    // TODO move into configuration?
    private static final long[] ALLOWED_PRICE_STEPS = new long[]{1, 5, 10, 25, 50, 100};
    private static final long[] ALLOWED_LOT_SIZES = new long[]{1, 10, 100};


    public static List<Pair<GeneratorSymbolSpec, Double>> generateRandomSymbols(final int num,
                                                                                final Map<Integer, Double> currenciesWeights,
                                                                                final EnumSet<GeneratorSymbolSpec.SymbolType> allowedSymbolTypes,
                                                                                final int symbolIdShift,
                                                                                final int seed) {
        final Random random = new Random(seed);
        final RandomGenerator rand = new JDKRandomGenerator(seed);

        final Supplier<GeneratorSymbolSpec.SymbolType> symbolTypeSupplier = RandomUtils.enumValuesSupplier(allowedSymbolTypes, random);

        if (currenciesWeights.size() < 2) {
            throw new IllegalArgumentException("need more than 2 currencies");
        }

        // build index to currency mapper
        final int[] idxToCurrency = currenciesWeights.keySet().stream().mapToInt(a -> a).toArray();

        // build currency indexes distribution
        final List<Pair<Integer, Double>> idxWeightPairs = IntStream.range(0, idxToCurrency.length)
                .mapToObj(idx -> Pair.create(idx, currenciesWeights.get(idxToCurrency[idx])))
                .collect(Collectors.toList());

        final EnumeratedDistribution<Integer> idxDistribution = new EnumeratedDistribution<>(rand, idxWeightPairs);

//        Map<Integer, EnumeratedDistribution<Integer>> baseDistributions = currenciesWeights.keySet().stream()
//                .collect(Collectors.toMap(
//                        currencyId -> currencyId,
//                        currencyId -> toDistribution(currenciesWeights, x -> x != currencyId, rand)));


        final double[] distribution = RandomUtils.paretoDistribution(num, rand);

        double weightSum = 0.0;

        final List<Pair<GeneratorSymbolSpec, Double>> result = new ArrayList<>();
        for (int i = 0; i < num; ) {

            // quote currency is Pareto-distributed
            final int index1 = idxDistribution.sample();
            final int quoteCurrency = idxToCurrency[index1];

            // base currency is evenly distributed
            // to avoid symbols baseCurrency==quoteCurrency, using n-1 range (with gap at index1)
            final int index2raw = random.nextInt(idxToCurrency.length - 1);
            final int index2 = index2raw != index1 ? index2raw : index2raw + 1;
            final int baseCurrency = idxToCurrency[index2];

            final GeneratorSymbolSpec.SymbolType type = symbolTypeSupplier.get();

            // taker fee >= maker fee
            final long makerFee = random.nextInt(1000);
            final long takerFee = makerFee + random.nextInt(500);

            // margin (very low to avoid NSF)
            final long marginBuy;
            final long marginSell;
            if (type == GeneratorSymbolSpec.SymbolType.FUTURES_CONTRACT) {
                marginBuy = random.nextInt(100) + 1L;
                marginSell = random.nextInt(100) + 1L;
            } else {
                marginBuy = 0L;
                marginSell = 0L;
            }

            final long baseScaleK = ALLOWED_LOT_SIZES[random.nextInt(ALLOWED_LOT_SIZES.length)];
            final long quoteScaleK = ALLOWED_PRICE_STEPS[random.nextInt(ALLOWED_PRICE_STEPS.length)];

            final GeneratorSymbolSpec symbol = new GeneratorSymbolSpec(
                    symbolIdShift + i,
                    type,
                    baseCurrency, // TODO for futures can be any value?
                    quoteCurrency,
                    baseScaleK,
                    quoteScaleK,
                    takerFee,
                    makerFee,
                    marginBuy,
                    marginSell);

            final double weight = distribution[i]
                    * currenciesWeights.get(baseCurrency)
                    * currenciesWeights.get(quoteCurrency);

            weightSum += weight;

            result.add(Pair.create(symbol, weight));

            //log.debug("{}", symbol);
            i++;

        }

        return normalize(weightSum, result);
    }

    private static List<Pair<GeneratorSymbolSpec, Double>> normalize(double weightSum,
                                                                     List<Pair<GeneratorSymbolSpec, Double>> list) {

        return list.stream()
                .map(p -> Pair.create(p.getFirst(), p.getSecond() / weightSum))
                .collect(Collectors.toList());
    }

//    private static EnumeratedDistribution<Integer> toDistribution(final Map<Integer, Double> currenciesWeights,
//                                                                  final IntPredicate filter,
//                                                                  final RandomGenerator rand) {
//        final List<Pair<Integer, Double>> weightPairs = currenciesWeights.entrySet().stream()
//                .filter(e -> filter.test(e.getKey()))
//                .map(e -> Pair.create(e.getKey(), e.getValue()))
//                .collect(Collectors.toList());
//
//        return new EnumeratedDistribution<>(rand, weightPairs);
//    }


}
