package exchange.core2.benchmarks.generator.orders;

import exchange.core2.benchmarks.generator.Constants;
import exchange.core2.benchmarks.generator.GeneratorSymbolSpec;
import exchange.core2.benchmarks.generator.clients.ClientsCurrencyAccountsGenerator;
import exchange.core2.benchmarks.generator.symbols.SymbolsGenerator;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MultiSymbolOrdersGeneratorTest {


    private static final Logger log = LoggerFactory.getLogger(MultiSymbolOrdersGeneratorTest.class);

    @Test
    public void generateMultipleSymbols() {


        final List<GeneratorSymbolSpec> specs = new ArrayList<>();
        specs.add(Constants.SYMBOLSPEC_EUR_USD);
        specs.add(Constants.SYMBOLSPECFEE_USD_JPY);
        specs.add(Constants.SYMBOLSPEC_ETH_XBT);
        specs.add(Constants.SYMBOLSPECFEE_XBT_LTC);

        final Set<Integer> currencies = new HashSet<>();
        currencies.add(Constants.CURRENECY_USD);
        currencies.add(Constants.CURRENECY_EUR);
        currencies.add(Constants.CURRENECY_JPY);
        currencies.add(Constants.CURRENECY_XBT);
        currencies.add(Constants.CURRENECY_LTC);
        currencies.add(Constants.CURRENECY_ETH);

        List<BitSet> accounts = ClientsCurrencyAccountsGenerator.generateClients(1000, currencies, 1);

        //accounts.forEach(s -> log.debug("{}", s));

        OrdersGeneratorConfig config = new OrdersGeneratorConfig(
                specs,
                100000,
                accounts,
                1000,
                1,
                false,
                OrdersGeneratorConfig.PreFillMode.ORDERS_NUMBER);


        MultiSymbolGenResult multiSymbolGenResult = MultiSymbolOrdersGenerator.generateMultipleSymbols(config);

        log.debug("getBenchmarkCommandsSize={}", multiSymbolGenResult.getBenchmarkCommandsSize());
        log.debug("CommandsFill length={}", multiSymbolGenResult.getCommandsFill().join().getRemainingSize());
        log.debug("CommandsBenchmark length={}", multiSymbolGenResult.getCommandsBenchmark().join().getRemainingSize());
        multiSymbolGenResult.getOrderBookHashes().forEach((s, h) -> log.debug("hash {}={}", s, h));


    }


    @Test
    public void generateMultipleLarge() {

        List<Integer> currencies = IntStream.rangeClosed(1, 20).boxed().collect(Collectors.toList());

        List<GeneratorSymbolSpec> specs = SymbolsGenerator.generateRandomSymbols(
                20_000,
                currencies,
                EnumSet.allOf(GeneratorSymbolSpec.SymbolType.class),
                1000,
                1);

//        specs.forEach(spec -> log.debug("{}", spec));


        List<BitSet> accounts = ClientsCurrencyAccountsGenerator.generateClients(100_000, currencies, 1);

        //accounts.forEach(s -> log.debug("{}", s));

        OrdersGeneratorConfig config = new OrdersGeneratorConfig(
                specs,
                1_000_000,
                accounts,
                100_000,
                1,
                false,
                OrdersGeneratorConfig.PreFillMode.ORDERS_NUMBER);


        MultiSymbolGenResult multiSymbolGenResult = MultiSymbolOrdersGenerator.generateMultipleSymbols(config);

        log.debug("getBenchmarkCommandsSize={}", multiSymbolGenResult.getBenchmarkCommandsSize());
        log.debug("CommandsFill length={}", multiSymbolGenResult.getCommandsFill().join().getRemainingSize());
        log.debug("CommandsBenchmark length={}", multiSymbolGenResult.getCommandsBenchmark().join().getRemainingSize());
//        multiSymbolGenResult.getOrderBookHashes().forEach((s, h) -> log.debug("hash {}={}", s, h));


    }
}