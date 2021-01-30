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

import exchange.core2.orderbook.ISymbolSpecification;

public final class GeneratorSymbolSpec implements ISymbolSpecification {

    private final int symbolId;
    private final SymbolType symbolType;

    // currency pair specification
    private final int baseCurrency;  // base currency // TODO baseAsset?
    private final int quoteCurrency; // quote/counter currency (OR futures contract currency)
    private final long baseScaleK;   // base currency amount multiplier (lot size in base currency units)
    private final long quoteScaleK;  // quote currency amount multiplier (step size in quote currency units)

    // fees per lot in quote? currency units
    private final long takerFee; // TODO check invariant: taker fee is not less than maker fee
    private final long makerFee;

    // margin settings (for type=FUTURES_CONTRACT only)
    private final long marginBuy;   // buy margin (quote currency)
    private final long marginSell;  // sell margin (quote currency)

    public GeneratorSymbolSpec(int symbolId,
                               SymbolType symbolType,
                               int baseCurrency,
                               int quoteCurrency,
                               long baseScaleK,
                               long quoteScaleK,
                               long takerFee,
                               long makerFee,
                               long marginBuy,
                               long marginSell) {

        this.symbolId = symbolId;
        this.symbolType = symbolType;
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
        this.baseScaleK = baseScaleK;
        this.quoteScaleK = quoteScaleK;
        this.takerFee = takerFee;
        this.makerFee = makerFee;
        this.marginBuy = marginBuy;
        this.marginSell = marginSell;
    }


    @Override
    public int getSymbolId() {
        return symbolId;
    }


    public SymbolType getSymbolType() {
        return symbolType;
    }

    public int getBaseCurrency() {
        return baseCurrency;
    }

    public int getQuoteCurrency() {
        return quoteCurrency;
    }

    public long getBaseScaleK() {
        return baseScaleK;
    }

    public long getQuoteScaleK() {
        return quoteScaleK;
    }

    public long getTakerFee() {
        return takerFee;
    }

    public long getMakerFee() {
        return makerFee;
    }

    public long getMarginBuy() {
        return marginBuy;
    }

    public long getMarginSell() {
        return marginSell;
    }

    @Override
    public boolean isExchangeType() {
        return symbolType == SymbolType.CURRENCY_EXCHANGE_PAIR;
    }

    @Override
    public int stateHash() {
        return 0; // TODO implement
    }

    public enum SymbolType {
        CURRENCY_EXCHANGE_PAIR,
        FUTURES_CONTRACT
    }

}