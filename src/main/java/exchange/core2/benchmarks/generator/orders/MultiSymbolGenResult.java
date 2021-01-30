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

import exchange.core2.orderbook.util.BufferWriter;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MultiSymbolGenResult {

    private final Map<Integer, Integer> orderBookHashes;

    // completable future (as merging takes some time)
    private final CompletableFuture<BufferWriter> commandsFill;
    private final CompletableFuture<BufferWriter> commandsBenchmark;

    private final int benchmarkCommandsSize;

    public Map<Integer, Integer> getOrderBookHashes() {
        return orderBookHashes;
    }

    public CompletableFuture<BufferWriter> getCommandsFill() {
        return commandsFill;
    }

    public CompletableFuture<BufferWriter> getCommandsBenchmark() {
        return commandsBenchmark;
    }

    public int getBenchmarkCommandsSize() {
        return benchmarkCommandsSize;
    }

    public MultiSymbolGenResult(Map<Integer, Integer> orderBookHashes,
                                CompletableFuture<BufferWriter> commandsFill,
                                CompletableFuture<BufferWriter> commandsBenchmark,
                                int benchmarkCommandsSize) {

        this.orderBookHashes = orderBookHashes;
        this.commandsFill = commandsFill;
        this.commandsBenchmark = commandsBenchmark;
        this.benchmarkCommandsSize = benchmarkCommandsSize;
    }
}
