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
package exchange.core2.benchmarks.generator;


import exchange.core2.benchmarks.generator.orders.OrdersGeneratorConfig;

import java.util.Set;

@Deprecated
public final class TestDataParameters {

    private final int totalTransactionsNumber;
    private final int targetOrderBookOrdersTotal;
    private final int numAccounts;
    private final Set<Integer> currenciesAllowed;
    private final int numSymbols;
    private final AllowedSymbolTypes allowedSymbolTypes;
    private final OrdersGeneratorConfig.PreFillMode preFillMode;
    private final boolean avalancheIOC;

    public TestDataParameters(int totalTransactionsNumber,
                              int targetOrderBookOrdersTotal,
                              int numAccounts, Set<Integer> currenciesAllowed,
                              int numSymbols,
                              AllowedSymbolTypes allowedSymbolTypes,
                              OrdersGeneratorConfig.PreFillMode preFillMode,
                              boolean avalancheIOC) {

        this.totalTransactionsNumber = totalTransactionsNumber;
        this.targetOrderBookOrdersTotal = targetOrderBookOrdersTotal;
        this.numAccounts = numAccounts;
        this.currenciesAllowed = currenciesAllowed;
        this.numSymbols = numSymbols;
        this.allowedSymbolTypes = allowedSymbolTypes;
        this.preFillMode = preFillMode;
        this.avalancheIOC = avalancheIOC;
    }

    public static TestDataParameters singlePairMargin() {

        return new TestDataParameters(
                3_000_000,
                1000,
                2000,
                Constants.CURRENCIES_FUTURES,
                1,
                AllowedSymbolTypes.FUTURES_CONTRACT,
                OrdersGeneratorConfig.PreFillMode.ORDERS_NUMBER,
                false);
    }


    public static TestDataParameters singlePairExchange() {

        return new TestDataParameters(
                3_000_000,
                1000,
                2000,
                Constants.CURRENCIES_EXCHANGE,
                1,
                AllowedSymbolTypes.CURRENCY_EXCHANGE_PAIR,
                OrdersGeneratorConfig.PreFillMode.ORDERS_NUMBER,
                false);
    }

    /**
     * - 1M active users (3M currency accounts)
     * - 1M pending limit-orders
     * - 10K symbols
     *
     * @return medium exchange test data configuration
     */
    public static TestDataParameters mediumBuilder() {

        return new TestDataParameters(
                3_000_000,
                1_000_000,
                3_300_000,
                Constants.ALL_CURRENCIES,
                10_000,
                AllowedSymbolTypes.BOTH,
                OrdersGeneratorConfig.PreFillMode.ORDERS_NUMBER_PLUS_QUARTER,
                false);
    }

    /**
     * - 3M active users (10M currency accounts)
     * - 3M pending limit-orders
     * - 50K symbols
     *
     * @return large exchange test data configuration
     */
    public static TestDataParameters largeBuilder() {

        return new TestDataParameters(
                3_000_000,
                3_000_000,
                10_000_000,
                Constants.ALL_CURRENCIES,
                50_000,
                AllowedSymbolTypes.BOTH,
                OrdersGeneratorConfig.PreFillMode.ORDERS_NUMBER,
                false);
    }

    /**
     * - 10M active users (33M currency accounts)
     * - 30M pending limit-orders
     * - 100K symbols
     *
     * @return huge exchange test data configuration
     */
    public static TestDataParameters hugeBuilder() {
        return new TestDataParameters(
                10_000_000,
                30_000_000,
                33_000_000,
                Constants.ALL_CURRENCIES,
                100_000,
                AllowedSymbolTypes.BOTH,
                OrdersGeneratorConfig.PreFillMode.ORDERS_NUMBER,
                false);
    }

    public enum AllowedSymbolTypes {
        FUTURES_CONTRACT,
        CURRENCY_EXCHANGE_PAIR,
        BOTH
    }

    public int getTotalTransactionsNumber() {
        return totalTransactionsNumber;
    }

    public int getTargetOrderBookOrdersTotal() {
        return targetOrderBookOrdersTotal;
    }

    public int getNumAccounts() {
        return numAccounts;
    }

    public Set<Integer> getCurrenciesAllowed() {
        return currenciesAllowed;
    }

    public int getNumSymbols() {
        return numSymbols;
    }

    public AllowedSymbolTypes getAllowedSymbolTypes() {
        return allowedSymbolTypes;
    }

    public OrdersGeneratorConfig.PreFillMode getPreFillMode() {
        return preFillMode;
    }

    public boolean isAvalancheIOC() {
        return avalancheIOC;
    }
}
