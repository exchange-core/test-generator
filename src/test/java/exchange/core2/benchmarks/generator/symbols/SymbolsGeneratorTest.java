package exchange.core2.benchmarks.generator.symbols;

import exchange.core2.benchmarks.generator.GeneratorSymbolSpec;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SymbolsGeneratorTest {

    private static final Logger log = LoggerFactory.getLogger(SymbolsGeneratorTest.class);


    @Test
    public void test() {

        List<Integer> currencies = IntStream.rangeClosed(1, 20).boxed().collect(Collectors.toList());

        List<GeneratorSymbolSpec> specs = SymbolsGenerator.generateRandomSymbols(
                10_000,
                currencies,
                EnumSet.allOf(GeneratorSymbolSpec.SymbolType.class),
                1000,
                1);

        specs.forEach(spec -> log.debug("{}", spec));

    }
}