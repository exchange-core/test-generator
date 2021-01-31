package exchange.core2.benchmarks.generator.currencies;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CurrenciesGeneratorTest {

    private static final Logger log = LoggerFactory.getLogger(CurrenciesGeneratorTest.class);

    @Test
    public void randomSymbols() {


        Map<Integer, Double> integerDoubleMap = CurrenciesGenerator.randomSymbols(100, 600, 1);

        integerDoubleMap.forEach((k, v) -> log.debug("{}: {}", k, v));

    }
}