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

import java.util.*;
import java.util.function.Supplier;

public final class SymbolsGenerator {

    // TODO move into configuration?
    private static final long[] ALLOWED_PRICE_STEPS = new long[]{1, 5, 10, 25, 50, 100};
    private static final long[] ALLOWED_LOT_SIZES = new long[]{1, 10, 100};


    public static List<GeneratorSymbolSpec> generateRandomSymbols(final int num,
                                                                  final Collection<Integer> currenciesAllowed,
                                                                  final EnumSet<GeneratorSymbolSpec.SymbolType> allowedSymbolTypes,
                                                                  final int symbolIdShift,
                                                                  final int seed) {
        final Random random = new Random(seed);

        final Supplier<GeneratorSymbolSpec.SymbolType> symbolTypeSupplier = RandomUtils.enumValuesSupplier(allowedSymbolTypes, random);

        if (new HashSet<>(currenciesAllowed).size() < 2) {
            throw new IllegalArgumentException("need more than 2 currencies");
        }

        final List<Integer> currenciesList = new ArrayList<>(currenciesAllowed);
        final List<GeneratorSymbolSpec> result = new ArrayList<>();
        for (int i = 0; i < num; ) {

            final int index1 = random.nextInt(currenciesList.size());
            final int baseCurrency = currenciesList.get(index1);

            // avoid symbols baseCurrency==quoteCurrency
            final int index2raw = random.nextInt(currenciesList.size() - 1);
            final int index2 = index2raw != index1 ? index2raw : currenciesList.size() - 1;
            final int quoteCurrency = currenciesList.get(index2);

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

            result.add(symbol);

            //log.debug("{}", symbol);
            i++;

        }
        return result;
    }

}
