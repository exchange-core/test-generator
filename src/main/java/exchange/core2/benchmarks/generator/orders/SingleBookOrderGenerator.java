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
import exchange.core2.orderbook.ISymbolSpecification;
import exchange.core2.orderbook.api.CommandResponse;
import exchange.core2.orderbook.api.CommandResponsePlace;
import exchange.core2.orderbook.api.OrderBookResponse;
import exchange.core2.orderbook.api.QueryResponseL2Data;
import exchange.core2.orderbook.naive.OrderBookNaiveImpl;
import exchange.core2.orderbook.util.BufferWriter;
import exchange.core2.orderbook.util.CommandsEncoder;
import exchange.core2.orderbook.util.ResponseDecoder;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.function.LongConsumer;
import java.util.function.UnaryOperator;


public class SingleBookOrderGenerator {


    private static final Logger log = LoggerFactory.getLogger(SingleBookOrderGenerator.class);


    public static final int CHECK_ORDERBOOK_STAT_EVERY_NTH_COMMAND = 512;
    public static final UnaryOperator<Integer> UID_PLAIN_MAPPER = i -> i + 1;

    public static GenResult generateCommands(
            final int benchmarkTransactionsNumber,
            final int targetOrderBookOrders,
            final int numUsers,
            final UnaryOperator<Integer> uidMapper,
            final ISymbolSpecification spec,
            final boolean enableSlidingPrice,
            final boolean avalancheIOC,
            final LongConsumer asyncProgressConsumer,
            final int orderIdCounter,
            final int seed) {


        final BufferWriter resultsBufferWriter = new BufferWriter(new ExpandableArrayBuffer(), 0);

        // TODO specify symbol type (for testing exchange-bid-move rejects)
        final IOrderBook<ISymbolSpecification> orderBook = new OrderBookNaiveImpl<>(spec, false, resultsBufferWriter);

        final Random rand = new Random(Long.hashCode(spec.getSymbolId() * -177277 + seed));

        final OrdersGeneratorSession session = new OrdersGeneratorSession(
                orderBook,
                targetOrderBookOrders,
                avalancheIOC,
                numUsers,
                uidMapper,
                enableSlidingPrice,
                orderIdCounter,
                rand);

        int nextSizeCheck = Math.min(CHECK_ORDERBOOK_STAT_EVERY_NTH_COMMAND, targetOrderBookOrders + 1);

        final int totalCommandsNumber = benchmarkTransactionsNumber + targetOrderBookOrders;

        int lastProgressReported = 0;

        for (int i = 0; i < totalCommandsNumber; i++) {

            final boolean fillStage = i < targetOrderBookOrders;


            final int lastWriterPosition;
            final BufferWriter commandBufferWriter;
            if (fillStage) {
                commandBufferWriter = session.fillCommandsBufferWriter;
                lastWriterPosition = commandBufferWriter.getWriterPosition();
                CommandGenerator.generateRandomGtcOrder(session, commandBufferWriter);
            } else {
                commandBufferWriter = session.benchmarkCommandsBufferWriter;
                lastWriterPosition = commandBufferWriter.getWriterPosition();
                CommandGenerator.generateRandomCommand(session, commandBufferWriter);
            }

            // log.debug("lastWriterPosition={}", lastWriterPosition);
            // log.debug("commandBufferWriter\n{}", commandBufferWriter.prettyHexDump());

            final MutableDirectBuffer commandsBuffer = commandBufferWriter.getBuffer();
            final byte cmdCode = commandsBuffer.getByte(lastWriterPosition);

            // log.debug("cmdCode:{}", cmdCode);

            switch (cmdCode) {
                case IOrderBook.COMMAND_PLACE_ORDER:
                    orderBook.newOrder(commandsBuffer, lastWriterPosition + 1, 1_000_000_000L + i);
                    break;

                case IOrderBook.COMMAND_CANCEL_ORDER:
                    orderBook.cancelOrder(commandsBuffer, lastWriterPosition + 1);
                    break;

                case IOrderBook.COMMAND_MOVE_ORDER:
                    orderBook.moveOrder(commandsBuffer, lastWriterPosition + 1);
                    break;

                case IOrderBook.COMMAND_REDUCE_ORDER:
                    orderBook.reduceOrder(commandsBuffer, lastWriterPosition + 1);
                    break;

                default:
                    throw new IllegalStateException("cmdCode=" + cmdCode);
            }

            // handler response from order book

            final OrderBookResponse orderBookResponse = ResponseDecoder.readResult(
                    resultsBufferWriter.getBuffer(),
                    resultsBufferWriter.getWriterPosition());
            resultsBufferWriter.reset();

            if (!orderBookResponse.isSuccessful()) {
                throw new IllegalStateException("Unsuccessful result code: " + orderBookResponse.toString());
            }

            matcherTradeEventEventHandler(
                    session,
                    (CommandResponse) orderBookResponse,
                    () -> updateOrderBookSizeStat(session, orderBook, resultsBufferWriter, fillStage));


            if (i >= nextSizeCheck) {

                nextSizeCheck += Math.min(CHECK_ORDERBOOK_STAT_EVERY_NTH_COMMAND, targetOrderBookOrders + 1);

                updateOrderBookSizeStat(session, orderBook, resultsBufferWriter, fillStage);
            }

            if (i % 10000 == 9999) {
                asyncProgressConsumer.accept(i - lastProgressReported);
                lastProgressReported = i;
            }
        }

        asyncProgressConsumer.accept(totalCommandsNumber - lastProgressReported);

        final QueryResponseL2Data responseL2Data = updateOrderBookSizeStat(session, orderBook, resultsBufferWriter, false);

        return new GenResult(
                responseL2Data,
                orderBook.stateHash(),
                session.fillCommandsBufferWriter.toReader(),
                targetOrderBookOrders,
                session.benchmarkCommandsBufferWriter.toReader(),
                benchmarkTransactionsNumber);
    }

    private static QueryResponseL2Data updateOrderBookSizeStat(final OrdersGeneratorSession session,
                                                               final IOrderBook<ISymbolSpecification> orderBook,
                                                               final BufferWriter resultsBufferWriter,
                                                               final boolean fillStage) {

        orderBook.sendL2Snapshot(CommandsEncoder.L2DataQuery(Integer.MAX_VALUE), 0);
        final QueryResponseL2Data responseL2Data = (QueryResponseL2Data) ResponseDecoder.readResult(
                resultsBufferWriter.getBuffer(),
                resultsBufferWriter.getWriterPosition());
        resultsBufferWriter.reset();

        // TODO move reduction into QueryResponseL2Data
        final int ordersNumAsk = responseL2Data.getAsks().stream().mapToInt(QueryResponseL2Data.L2Record::getOrders).sum();
        final int ordersNumBid = responseL2Data.getBids().stream().mapToInt(QueryResponseL2Data.L2Record::getOrders).sum();

        // log.debug("ask={}, bif={} seq={} filledAtSeq={}", ordersNumAsk, ordersNumBid, session.seq, session.filledAtSeq);

        // regulating OB size
        session.lastOrderBookOrdersSizeAsk = ordersNumAsk;
        session.lastOrderBookOrdersSizeBid = ordersNumBid;
//        log.debug("ordersNum:{}", ordersNum);

        if (session.avalancheIOC) {
            session.lastTotalVolumeAsk = responseL2Data.getAsks().stream().mapToLong(QueryResponseL2Data.L2Record::getVolume).sum();
            session.lastTotalVolumeBid = responseL2Data.getBids().stream().mapToLong(QueryResponseL2Data.L2Record::getVolume).sum();
        }

        // record stat snapshots
        if (!fillStage) {
            session.orderBookSizeAskStat.add(responseL2Data.getAsks().size());
            session.orderBookSizeBidStat.add(responseL2Data.getBids().size());
            session.orderBookNumOrdersAskStat.add(ordersNumAsk);
            session.orderBookNumOrdersBidStat.add(ordersNumBid);
        }

        return responseL2Data;
    }

    private static void matcherTradeEventEventHandler(final OrdersGeneratorSession session,
                                                      final CommandResponse commandResponse,
                                                      final Runnable updateOrderBookSizeStat) {

        final int orderId = (int) commandResponse.getOrderId();

        if (commandResponse.isOrderCompleted()) {
            session.orderUids.remove(orderId);
            session.numCompleted++;
        }

        commandResponse.getTrades().forEach(ev -> {
            final int makerOrderId = (int) ev.getMakerOrderId();
            final int tradeVolume = (int) -ev.getTradeSize();

            if (ev.isMakerOrderCompleted()) {
                session.orderUids.remove(makerOrderId);
                session.numCompleted++;
            }

            // decrease size (important for reduce operation)

            if (session.orderSizes.addToValue(makerOrderId, tradeVolume) < 0) {
                throw new IllegalStateException("Incorrect filled size for maker order " + makerOrderId);
            }

            int takerRemaining = session.orderSizes.addToValue(orderId, tradeVolume);
            if (takerRemaining < 0) {
                throw new IllegalStateException("Incorrect filled size for taker order " + orderId);
            }

            // process trade prices to adjust price general movement direction

            final long tradePrice = ev.getTradePrice();
            session.lastTradePrice = Math.min(session.maxPrice, Math.max(session.minPrice, tradePrice));

            if (tradePrice <= session.minPrice) {
                session.priceDirection = 1;
            } else if (tradePrice >= session.maxPrice) {
                session.priceDirection = -1;
            }
        });

        commandResponse.getReduceEventOpt().ifPresent(ev -> {
            int takerRemaining = session.orderSizes.addToValue(orderId, (int) ev.getReducedSize());
            if (takerRemaining < 0) {
                throw new IllegalStateException("Incorrect filled size for order " + orderId);
            }

            if (commandResponse instanceof CommandResponsePlace) {
                // treat reduce on placing as a rejection
                session.numRejected++;
                // force updating order book stat to push generator to issue more limit orders
                updateOrderBookSizeStat.run();
            } else {
                session.numReduced++;
            }
        });
    }
}
