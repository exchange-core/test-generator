/*
 * Copyright 2020 Maksim Zheravin
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
import org.apache.commons.math3.util.Pair;

import java.util.BitSet;
import java.util.List;
import java.util.function.Function;

public final class OrdersGeneratorConfig {

    private final List<Pair<GeneratorSymbolSpec, Double>> coreSymbolSpecifications;
    private final int totalTransactionsNumber;
    private final List<BitSet> usersAccounts;
    private final int targetOrderBookOrdersTotal;
    private final int seed;
    private final boolean avalancheIOC;
    private final PreFillMode preFillMode;

    public enum PreFillMode {

        ORDERS_NUMBER(OrdersGeneratorConfig::getTargetOrderBookOrdersTotal),
        ORDERS_NUMBER_PLUS_QUARTER(config -> config.targetOrderBookOrdersTotal * 5 / 4); // for snapshot tests to let some margin positions open

        public Function<OrdersGeneratorConfig, Integer> getCalculateReadySeqFunc() {
            return calculateReadySeqFunc;
        }

        PreFillMode(Function<OrdersGeneratorConfig, Integer> calculateReadySeqFunc) {
            this.calculateReadySeqFunc = calculateReadySeqFunc;
        }

        private final Function<OrdersGeneratorConfig, Integer> calculateReadySeqFunc;
    }

    public OrdersGeneratorConfig(List<Pair<GeneratorSymbolSpec, Double>> coreSymbolSpecifications,
                                 int totalTransactionsNumber,
                                 List<BitSet> usersAccounts,
                                 int targetOrderBookOrdersTotal,
                                 int seed,
                                 boolean avalancheIOC,
                                 PreFillMode preFillMode) {

        this.coreSymbolSpecifications = coreSymbolSpecifications;
        this.totalTransactionsNumber = totalTransactionsNumber;
        this.usersAccounts = usersAccounts;
        this.targetOrderBookOrdersTotal = targetOrderBookOrdersTotal;
        this.seed = seed;
        this.avalancheIOC = avalancheIOC;
        this.preFillMode = preFillMode;
    }

    public List<Pair<GeneratorSymbolSpec, Double>> getCoreSymbolSpecifications() {
        return coreSymbolSpecifications;
    }

    public int getTotalTransactionsNumber() {
        return totalTransactionsNumber;
    }

    public List<BitSet> getUsersAccounts() {
        return usersAccounts;
    }

    public int getTargetOrderBookOrdersTotal() {
        return targetOrderBookOrdersTotal;
    }

    public int getSeed() {
        return seed;
    }

    public boolean isAvalancheIOC() {
        return avalancheIOC;
    }

    public PreFillMode getPreFillMode() {
        return preFillMode;
    }
}
