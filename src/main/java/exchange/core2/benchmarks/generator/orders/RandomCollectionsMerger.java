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

import exchange.core2.orderbook.IOrderBook;
import exchange.core2.orderbook.util.BufferReader;
import exchange.core2.orderbook.util.BufferWriter;
import org.agrona.ExpandableArrayBuffer;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RandomCollectionsMerger {

    private static final Logger log = LoggerFactory.getLogger(RandomCollectionsMerger.class);


    public static BufferWriter mergeCommands(final Map<Integer, GenResult> genResults,
                                             final Function<GenResult, BufferReader> mapper,
                                             final RandomGenerator rand) {

        final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer();
        final BufferWriter bufferWriter = new BufferWriter(buffer, 0);

        // initial weight pairs
        List<Pair<SourceRecord, Double>> weightPairs = genResults.entrySet().stream()
                .map(entry -> {
                    final BufferReader reader = mapper.apply(entry.getValue());
                    final SourceRecord sourceRecord = new SourceRecord(reader, entry.getKey());
                    return new Pair<>(sourceRecord, (double) reader.getRemainingSize());
                })
                .collect(Collectors.toList());

        while (!weightPairs.isEmpty()) {

            // weightPairs.forEach(pair->log.debug("wp={}", pair));

            // rebuild distribution
            final EnumeratedDistribution<SourceRecord> ed = new EnumeratedDistribution<>(rand, weightPairs);

            // take random elements until face too many misses
            int missCounter = 0;
            while (missCounter < 3) {

                final SourceRecord sourceRecord = ed.sample();
                BufferReader reader = sourceRecord.bufferReader;

                if (reader.getRemainingSize() > 0) {
                    // copy
                    final byte cmdCode = reader.readByte();
                    bufferWriter.appendByte(cmdCode);
                    bufferWriter.appendInt(sourceRecord.symbolId);
                    reader.readBytesToWriter(bufferWriter, IOrderBook.fixedCommandSize(cmdCode));
                    missCounter = 0;
                } else {
                    missCounter++;
                }
            }

            // as empty queues leading to misses - rebuild wight pairs without them
            weightPairs = weightPairs.stream()
                    .filter(p -> p.getFirst().bufferReader.getRemainingSize() > 0)
                    .map(p -> Pair.create(p.getFirst(), (double) p.getFirst().bufferReader.getRemainingSize()))
                    .collect(Collectors.toList());

            // log.debug("rebuild size {}", weightPairs.size());
        }

        return bufferWriter;
    }

    private final static class SourceRecord {

        private final BufferReader bufferReader;
        private final int symbolId;

        private SourceRecord(BufferReader bufferReader, int symbolId) {
            this.bufferReader = bufferReader;
            this.symbolId = symbolId;
        }
    }

}
