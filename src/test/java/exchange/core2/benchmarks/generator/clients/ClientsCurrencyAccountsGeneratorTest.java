package exchange.core2.benchmarks.generator.clients;

import exchange.core2.benchmarks.generator.GeneratorSymbolSpec;
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.BitSet;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ClientsCurrencyAccountsGeneratorTest {

    private static final Logger log = LoggerFactory.getLogger(ClientsCurrencyAccountsGeneratorTest.class);

    @Test
    public void shouldGenerateUsers() {

        final List<Integer> allowedCurrencies = IntStream.rangeClosed(1, 20).boxed().collect(Collectors.toList());

        final List<BitSet> accounts = ClientsCurrencyAccountsGenerator.generateClients(100, allowedCurrencies, 1);

        accounts.forEach(currencies -> log.debug("{}", currencies));
    }


    @Test
    public void shouldGenerateUsersLarge() {

        final List<Integer> allowedCurrencies = IntStream.rangeClosed(1, 50).boxed().collect(Collectors.toList());

        final List<BitSet> accounts = ClientsCurrencyAccountsGenerator.generateClients(1_000_000, allowedCurrencies, 1);

        //accounts.forEach(currencies -> log.debug("{}", currencies));

        allowedCurrencies.forEach(c1 -> {
            IntIntHashMap counters = new IntIntHashMap();

            accounts.stream().filter(a -> a.get(c1)).forEach(a -> {
                a.stream().forEach(c2 -> counters.addToValue(c2, 1));
            });

            log.debug("{}: {}", c1, counters);

        });


    }


    @Test
    public void shouldCreateClientsListForSymbol() {

        final List<Integer> allowedCurrencies = IntStream.rangeClosed(1, 100).boxed().collect(Collectors.toList());

        final List<BitSet> accounts = ClientsCurrencyAccountsGenerator.generateClients(1000000, allowedCurrencies, 1);


        final long totalAccountsCreated = accounts.stream().mapToInt(BitSet::cardinality).sum();

        log.debug("clients={} accounts={}", accounts.size(), totalAccountsCreated);

        final GeneratorSymbolSpec spec = new GeneratorSymbolSpec(
                2000,
                GeneratorSymbolSpec.SymbolType.CURRENCY_EXCHANGE_PAIR,
                22,
                5,
                10,
                10,
                0,
                0);

        int[] uids = ClientsCurrencyAccountsGenerator.createClientsListForSymbol(
                accounts,
                spec,
                200000,
                1);

//        log.debug("{}", Arrays.toString(uids));
        log.debug("selected clients {}", uids.length);
        Arrays.stream(uids).forEach(uid -> {
            BitSet accs = accounts.get(uid);
//            log.debug("{}: {}", uid, accs);
        });

        IntSummaryStatistics stat = Arrays.stream(uids).map(uid -> accounts.get(uid).cardinality()).summaryStatistics();

        log.debug("stat: {}", stat);


    }

}