package exchange.core2.benchmarks.generator.symbols;

import exchange.core2.benchmarks.generator.GeneratorSymbolSpec;
import exchange.core2.benchmarks.generator.currencies.CurrenciesGenerator;
import org.apache.commons.math3.util.Pair;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public class SymbolsGeneratorTest {

    private static final Logger log = LoggerFactory.getLogger(SymbolsGeneratorTest.class);


    @Test
    public void test() {

        Map<Integer, Double> currencies = CurrenciesGenerator.randomCurrencies(20, 100, 1);

        currencies.forEach((k, v) -> log.debug("{}: {}", k, v));

        List<Pair<GeneratorSymbolSpec, Double>> specs = SymbolsGenerator.generateRandomSymbols(
                10_000,
                currencies,
                EnumSet.allOf(GeneratorSymbolSpec.SymbolType.class),
                1000,
                1);

        // specs.forEach(spec -> log.debug("{}", spec));

    }
}