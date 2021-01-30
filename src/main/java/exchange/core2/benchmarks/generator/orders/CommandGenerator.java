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
import exchange.core2.orderbook.OrderAction;
import exchange.core2.orderbook.util.BufferWriter;
import exchange.core2.orderbook.util.CommandsEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class CommandGenerator {

    public static final double CENTRAL_MOVE_ALPHA = 0.01;

    private static final Logger log = LoggerFactory.getLogger(CommandGenerator.class);


    public static void generateRandomCommand(OrdersGeneratorSession session, BufferWriter commandBufferWriter) {

        final OrderAction action = (session.rand.nextInt(4) + session.priceDirection >= 2)
                ? OrderAction.BID
                : OrderAction.ASK;

        final int lackOfOrders = (action == OrderAction.ASK)
                ? session.targetOrderBookOrdersHalf - session.lastOrderBookOrdersSizeAsk
                : session.targetOrderBookOrdersHalf - session.lastOrderBookOrdersSizeBid;

        final boolean requireFastFill = lackOfOrders > session.lackOrOrdersFastFillThreshold;
        final boolean growOrders = lackOfOrders > 0;

        // log.debug("{} growOrders={} requireFastFill={} lackOfOrders({})={}", session.orderIdCounter, growOrders, requireFastFill, action, lackOfOrders);

        final int q = session.rand.nextInt(growOrders
                ? (requireFastFill ? 2 : 10)
                : 40);

        if (q < 2 || session.orderUids.isEmpty()) {

            if (growOrders) {
                generateRandomGtcOrder(session, commandBufferWriter);
            } else {
                generateRandomInstantOrder(session, commandBufferWriter);
            }
            return;
        }

        // TODO improve random picking performance (custom hashset implementation?)
//        long t = System.nanoTime();
        final int size = Math.min(session.orderUids.size(), 512);

        final int randPos = session.rand.nextInt(size);
        Iterator<Map.Entry<Integer, Integer>> iterator = session.orderUids.entrySet().iterator();

        Map.Entry<Integer, Integer> rec = iterator.next();
        for (int i = 0; i < randPos; i++) {
            rec = iterator.next();
        }
//        session.hdrRecorder.recordValue(Math.min(System.nanoTime() - t, Integer.MAX_VALUE));
        final int orderId = rec.getKey();

        final int uid = rec.getValue();
        if (uid == 0) {
            throw new IllegalStateException();
        }

        if (q == 2) {
            session.orderUids.remove(orderId);
            commandBufferWriter.appendByte(IOrderBook.COMMAND_CANCEL_ORDER);
            CommandsEncoder.cancel(commandBufferWriter, orderId, uid);

        } else if (q == 3) {

            final int prevSize = session.orderSizes.get(orderId);
            final int reduceBy = session.rand.nextInt(prevSize) + 1;
            commandBufferWriter.appendByte(IOrderBook.COMMAND_REDUCE_ORDER);
            CommandsEncoder.reduce(commandBufferWriter, orderId, uid, reduceBy);

        } else {
            final int prevPrice = session.orderPrices.get(orderId);
            if (prevPrice == 0) {
                throw new IllegalStateException();
            }

            final double priceMove = (session.lastTradePrice - prevPrice) * CENTRAL_MOVE_ALPHA;
            int priceMoveRounded;
            if (prevPrice > session.lastTradePrice) {
                priceMoveRounded = (int) Math.floor(priceMove);
            } else if (prevPrice < session.lastTradePrice) {
                priceMoveRounded = (int) Math.ceil(priceMove);
            } else {
                priceMoveRounded = session.rand.nextInt(2) * 2 - 1;
            }

            final int newPrice = Math.min(prevPrice + priceMoveRounded, (int) session.maxPrice);
            // todo add min limit

            // log.debug("session.seq={} orderId={} size={} p={}", session.seq, orderId, session.actualOrders.size(), priceMoveRounded);

            session.orderPrices.put(orderId, newPrice);

            commandBufferWriter.appendByte(IOrderBook.COMMAND_MOVE_ORDER);

            CommandsEncoder.move(commandBufferWriter, orderId, (int) (long) uid, newPrice);
        }
    }

    public static void generateRandomGtcOrder(final OrdersGeneratorSession session,
                                              final BufferWriter commandBufferWriter) {

        final Random rand = session.rand;

        final OrderAction action = (rand.nextInt(4) + session.priceDirection >= 2) ? OrderAction.BID : OrderAction.ASK;
        final int uid = randomUid(session, rand);

        final int newOrderId = session.orderIdCounter++;

        final int dev = 1 + (int) (Math.pow(rand.nextDouble(), 2) * session.priceDeviation);

        long p = 0;
        final int x = 4;
        for (int i = 0; i < x; i++) {
            p += rand.nextInt(dev);
        }
        p = p / x * 2 - dev;
        if (p > 0 ^ action == OrderAction.ASK) {
            p = -p;
        }

        //log.debug("p={} action={}", p, action);
        final int price = (int) session.lastTradePrice + (int) p;

        final int size = 1 + rand.nextInt(6) * rand.nextInt(6) * rand.nextInt(6);


        session.orderPrices.put(newOrderId, price);
        session.orderSizes.put(newOrderId, size);
        session.orderUids.put(newOrderId, uid);

        final int userCookie = rand.nextInt();

        commandBufferWriter.appendByte(IOrderBook.COMMAND_PLACE_ORDER);

        CommandsEncoder.placeOrder(
                commandBufferWriter,
                IOrderBook.ORDER_TYPE_GTC,
                newOrderId,
                uid,
                price,
                action == OrderAction.BID ? session.maxPrice : 0,// set limit price
                size,
                action,
                userCookie);
    }

    public static void generateRandomInstantOrder(final OrdersGeneratorSession session,
                                                  final BufferWriter commandBufferWriter) {

        final Random rand = session.rand;

        final OrderAction action = (rand.nextInt(4) + session.priceDirection >= 2) ? OrderAction.BID : OrderAction.ASK;

        final int uid = randomUid(session, rand);

        final int newOrderId = session.orderIdCounter++;

        final long priceLimit = action == OrderAction.BID ? session.maxPrice : session.minPrice;

        final long size;
        final byte orderType;
        final long priceOrBudget;
        final long reserveBidPrice;

        if (session.avalancheIOC) {

            // just match with available liquidity

            orderType = IOrderBook.ORDER_TYPE_IOC;
            priceOrBudget = priceLimit;
            reserveBidPrice = action == OrderAction.BID ? session.maxPrice : 0; // set limit price
            final long availableVolume = action == OrderAction.ASK ? session.lastTotalVolumeAsk : session.lastTotalVolumeBid;

            long bigRand = rand.nextLong();
            bigRand = bigRand < 0 ? -1 - bigRand : bigRand;
            size = 1 + bigRand % (availableVolume + 1);

            if (action == OrderAction.ASK) {
                session.lastTotalVolumeAsk = Math.max(session.lastTotalVolumeAsk - size, 0);
            } else {
                session.lastTotalVolumeBid = Math.max(session.lastTotalVolumeAsk - size, 0);
            }
//                    log.debug("huge size={} at {}", placeCmd.size, session.seq);

        } else if (rand.nextInt(32) == 0) {

            // IOC:FOKB = 31:1
            orderType = IOrderBook.ORDER_TYPE_FOK_BUDGET;
            size = 1 + rand.nextInt(8) * rand.nextInt(8) * rand.nextInt(8);

            // set budget-expectation
            priceOrBudget = size * priceLimit;
            reserveBidPrice = priceOrBudget;

        } else {

            orderType = IOrderBook.ORDER_TYPE_IOC;
            priceOrBudget = priceLimit;
            reserveBidPrice = action == OrderAction.BID ? session.maxPrice : 0; // set limit price
            size = 1 + rand.nextInt(6) * rand.nextInt(6) * rand.nextInt(6);
        }

        session.orderSizes.put(newOrderId, (int) size);

        final int userCookie = rand.nextInt();

        commandBufferWriter.appendByte(IOrderBook.COMMAND_PLACE_ORDER);

        CommandsEncoder.placeOrder(
                commandBufferWriter,
                orderType,
                newOrderId,
                uid,
                priceOrBudget,
                reserveBidPrice,
                size,
                action,
                userCookie);
    }

    private static int randomUid(OrdersGeneratorSession session, Random rand) {

        final int uid = session.uidMapper.apply(rand.nextInt(session.numUsers));
        if (uid == 0) {
            throw new IllegalArgumentException("uid can not be 0, check uid mapping use UID_PLAIN_MAPPER");
        } else {
            return uid;
        }
    }
}
