package exchange.core2.benchmarks.generator.currencies;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CurrenciesGeneratorTest {

    private static final Logger log = LoggerFactory.getLogger(CurrenciesGeneratorTest.class);

    @Test
    public void testCurrenciesGenerator() {


        final Map<Integer, Double> currencies = CurrenciesGenerator.randomCurrencies(
                100, 600, 1);

        currencies.forEach((k, v) -> log.debug("{}: {}", k, v));

    }

    @Test
    public void testRatesGenerator() {

        final Map<Integer, Double> currencies = CurrenciesGenerator.randomCurrencies(
                30, 20, 912341322);

        final Map<Integer, Double> rates = CurrenciesGenerator.generateRandomRates(
                currencies.keySet().stream(), 2.2, 1823329877);

        rates.forEach((k, v) -> log.debug("{}: rate {}", k, v));
    }

    @Test
    public void testRatesMatrix() {

        final Map<Integer, Double> currencies = CurrenciesGenerator.randomCurrencies(
                5, 20, 122342);

        final Map<Integer, Double> rates = CurrenciesGenerator.generateRandomRates(
                currencies.keySet().stream(), 2.2, 4242342);

        rates.forEach((k, v) -> log.debug("{}: rate {}", k, v));

        final Map<Integer, Map<Integer, Double>> ratesMatrix = CurrenciesGenerator.createRatesMatrix(rates);

        ratesMatrix.forEach((currencyFrom, map) ->
                map.forEach((currencyTo, rate) ->
                        log.debug("{}->{} rate {}", currencyFrom, currencyTo, rate)));

    }


}