package exchange.core2.benchmarks.generator.orders;

import exchange.core2.benchmarks.generator.Constants;
import exchange.core2.benchmarks.generator.GeneratorSymbolSpec;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingleBookOrderGeneratorTest {

    private static final Logger log = LoggerFactory.getLogger(SingleBookOrderGeneratorTest.class);

    @Test
    public void shouldGenerateCommands() {


        final GeneratorSymbolSpec spec = Constants.SYMBOLSPECFEE_XBT_LTC;

        final GenResult genResult = SingleBookOrderGenerator.generateCommands(
                100_000,
                1000,
                1000,
                i -> i + 1,
                spec,
                false,
                false,
                a -> {
                },
                1,
                1);

        log.debug("benchmark size: {}", genResult.getCommandsBenchmark().getSize());


    }

}