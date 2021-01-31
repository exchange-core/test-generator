package exchange.core2.benchmarks.generator.currencies;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CurrenciesGeneratorTest {

    private static final Logger log = LoggerFactory.getLogger(CurrenciesGeneratorTest.class);

    @Test
    public void test() {


        Map<Integer, Double> currencies = CurrenciesGenerator.randomCurrencies(100, 600, 1);

        currencies.forEach((k, v) -> log.debug("{}: {}", k, v));

    }
}