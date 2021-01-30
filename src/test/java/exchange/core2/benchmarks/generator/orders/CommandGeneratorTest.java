package exchange.core2.benchmarks.generator.orders;

import exchange.core2.benchmarks.generator.Constants;
import exchange.core2.benchmarks.generator.GeneratorSymbolSpec;
import exchange.core2.orderbook.IOrderBook;
import exchange.core2.orderbook.ISymbolSpecification;
import exchange.core2.orderbook.naive.OrderBookNaiveImpl;
import exchange.core2.orderbook.util.BufferReader;
import exchange.core2.orderbook.util.BufferWriter;
import org.agrona.BitUtil;
import org.agrona.ExpandableArrayBuffer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

import static exchange.core2.orderbook.IOrderBook.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CommandGeneratorTest {


    private static final Logger log = LoggerFactory.getLogger(CommandGeneratorTest.class);

    @Test
    public void test() {

        final GeneratorSymbolSpec spec = Constants.SYMBOLSPECFEE_XBT_LTC;

        final ExpandableArrayBuffer resultsBuffer = new ExpandableArrayBuffer();
        final BufferWriter resultsBufferWriter = new BufferWriter(resultsBuffer, 0);

        // TODO specify symbol type (for testing exchange-bid-move rejects)
        final IOrderBook<ISymbolSpecification> orderBook = new OrderBookNaiveImpl<>(spec, false, resultsBufferWriter);

        final Random rand = new Random(1L);

        final OrdersGeneratorSession session = new OrdersGeneratorSession(
                orderBook,
                20,
                false,
                2000,
                i -> i,
                false,
                693,
                rand);

        CommandGenerator.generateRandomGtcOrder(session, session.fillCommandsBufferWriter);

        BufferReader buffer = session.fillCommandsBufferWriter.toReader();

        assertThat(buffer.getSize(), is(BitUtil.SIZE_OF_BYTE + IOrderBook.PLACE_OFFSET_END));


        final long cmd = buffer.getByte(0);
        final int offset = BitUtil.SIZE_OF_BYTE;

        final long uid = buffer.getLong(offset + PLACE_OFFSET_UID);
        final long orderId = buffer.getLong(offset + PLACE_OFFSET_ORDER_ID);
        final long price = buffer.getLong(offset + PLACE_OFFSET_PRICE);
        final long reservedBigPrice = buffer.getLong(offset + PLACE_OFFSET_RESERVED_BID_PRICE);
        final long size = buffer.getLong(offset + PLACE_OFFSET_SIZE);
        final int userCookie = buffer.getInt(offset + PLACE_OFFSET_USER_COOKIE);
        final byte action = buffer.getByte(offset + PLACE_OFFSET_ACTION);
        final byte type = buffer.getByte(offset + PLACE_OFFSET_TYPE);

        log.debug("cmd={} uid={} orderId={} price={} reservedBigPrice={} size={} userCookie={}, action={}, type={}",
                cmd, uid, orderId, price, reservedBigPrice, size, userCookie, action, type);

//        assertTrue(uid >= 0 && uid < 1000);


    }

}