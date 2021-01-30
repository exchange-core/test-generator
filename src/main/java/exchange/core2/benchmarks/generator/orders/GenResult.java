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

import exchange.core2.orderbook.api.QueryResponseL2Data;
import exchange.core2.orderbook.util.BufferReader;

public class GenResult {

    private final QueryResponseL2Data finalOrderBookSnapshot;
    private final int finalOrderBookHash;

    private final BufferReader commandsFill;
    private final int numCommandsFill;

    private final BufferReader commandsBenchmark;
    private final int numCommandsBenchmark;

    public GenResult(final QueryResponseL2Data finalOrderBookSnapshot,
                     final int finalOrderBookHash,
                     final BufferReader commandsFill,
                     final int numCommandsFill,
                     final BufferReader commandsBenchmark,
                     final int numCommandsBenchmark) {

        this.finalOrderBookSnapshot = finalOrderBookSnapshot;
        this.finalOrderBookHash = finalOrderBookHash;
        this.commandsFill = commandsFill;
        this.numCommandsFill = numCommandsFill;
        this.commandsBenchmark = commandsBenchmark;
        this.numCommandsBenchmark = numCommandsBenchmark;
    }

    public QueryResponseL2Data getFinalOrderBookSnapshot() {
        return finalOrderBookSnapshot;
    }

    public int getFinalOrderBookHash() {
        return finalOrderBookHash;
    }

    public BufferReader getCommandsFill() {
        return commandsFill;
    }

    public int getNumCommandsFill() {
        return numCommandsFill;
    }

    public BufferReader getCommandsBenchmark() {
        return commandsBenchmark;
    }

    public int getNumCommandsBenchmark() {
        return numCommandsBenchmark;
    }

    public int size() {
        return commandsFill.getSize() + commandsBenchmark.getSize();
    }

}
