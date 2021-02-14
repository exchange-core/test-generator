package exchange.core2.benchmarks.generator.orders;

import exchange.core2.benchmarks.generator.Constants;
import exchange.core2.benchmarks.generator.GeneratorSymbolSpec;
import exchange.core2.benchmarks.generator.clients.ClientsCurrencyAccountsGenerator;
import exchange.core2.benchmarks.generator.currencies.CurrenciesGenerator;
import exchange.core2.benchmarks.generator.symbols.SymbolsGenerator;
import org.apache.commons.math3.util.Pair;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MultiSymbolOrdersGeneratorTest {


    private static final Logger log = LoggerFactory.getLogger(MultiSymbolOrdersGeneratorTest.class);

    @Test
    public void generateMultipleSymbols() {


        final List<Pair<GeneratorSymbolSpec, Double>> specs = new ArrayList<>();
        specs.add(Pair.create(Constants.SYMBOLSPEC_EUR_USD, 0.25));
        specs.add(Pair.create(Constants.SYMBOLSPECFEE_USD_JPY, 0.25));
        specs.add(Pair.create(Constants.SYMBOLSPEC_ETH_XBT, 0.25));
        specs.add(Pair.create(Constants.SYMBOLSPECFEE_XBT_LTC, 0.25));

        final Map<Integer, Double> currencies = new HashMap<>();
        currencies.put(Constants.CURRENECY_USD, 0.4);
        currencies.put(Constants.CURRENECY_EUR, 0.2);
        currencies.put(Constants.CURRENECY_JPY, 0.1);
        currencies.put(Constants.CURRENECY_XBT, 0.1);
        currencies.put(Constants.CURRENECY_LTC, 0.1);
        currencies.put(Constants.CURRENECY_ETH, 0.1);

        List<BitSet> accounts = ClientsCurrencyAccountsGenerator.generateClients(1000, currencies, 1);

        //accounts.forEach(s -> log.debug("{}", s));

        MultiSymbolGenResult multiSymbolGenResult = MultiSymbolOrdersGenerator.generateMultipleSymbols(
                specs,
                100000,
                accounts,
                1000,
                1,
                false);

        log.debug("getBenchmarkCommandsSize={}", multiSymbolGenResult.getBenchmarkCommandsSize());
        log.debug("CommandsFill length={}", multiSymbolGenResult.getCommandsFill().join().getRemainingSize());
        log.debug("CommandsBenchmark length={}", multiSymbolGenResult.getCommandsBenchmark().join().getRemainingSize());
        multiSymbolGenResult.getOrderBookHashes().forEach((s, h) -> log.debug("hash {}={}", s, h));


    }


    @Test
    public void generateMultipleLarge() {

        Map<Integer, Double> currencies = CurrenciesGenerator.randomCurrencies(20, 600, 1);

//        currencies.forEach((k, v) -> log.debug("{}: {}", k, v));


        List<Pair<GeneratorSymbolSpec, Double>> specs = SymbolsGenerator.generateRandomSymbols(
                20_000,
                currencies,
                EnumSet.allOf(GeneratorSymbolSpec.SymbolType.class),
                1000,
                1);

//        specs.forEach(spec -> log.debug("{}", spec));


        List<BitSet> accounts = ClientsCurrencyAccountsGenerator.generateClients(100_000, currencies, 1);

        //accounts.forEach(s -> log.debug("{}", s));

        MultiSymbolGenResult multiSymbolGenResult = MultiSymbolOrdersGenerator.generateMultipleSymbols(
                specs,
                1_000_000,
                accounts,
                100_000,
                1,
                false);

        log.debug("getBenchmarkCommandsSize={}", multiSymbolGenResult.getBenchmarkCommandsSize());
        log.debug("CommandsFill length={}", multiSymbolGenResult.getCommandsFill().join().getRemainingSize());
        log.debug("CommandsBenchmark length={}", multiSymbolGenResult.getCommandsBenchmark().join().getRemainingSize());
//        multiSymbolGenResult.getOrderBookHashes().forEach((s, h) -> log.debug("hash {}={}", s, h));


    }
}