package exchange.core2.benchmarks.generator.clients;

import exchange.core2.benchmarks.generator.GeneratorSymbolSpec;
import exchange.core2.benchmarks.generator.currencies.CurrenciesGenerator;
import org.agrona.collections.Hashing;
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ClientsCurrencyAccountsGeneratorTest {

    private static final Logger log = LoggerFactory.getLogger(ClientsCurrencyAccountsGeneratorTest.class);

    @Test
    public void shouldGenerateUsers() {

        final Map<Integer, Double> currencies = CurrenciesGenerator.randomCurrencies(20, 1, 1);
        currencies.forEach((k, v) -> log.debug("{}: {}", k, v));

        final List<BitSet> accounts = ClientsCurrencyAccountsGenerator.generateClients(100, currencies, 1);
        accounts.forEach(acc -> log.debug("{}", acc));
    }


    @Test
    public void shouldGenerateUsersLarge() {

        final Map<Integer, Double> allowedCurrencies = CurrenciesGenerator.randomCurrencies(50, 1, 1);

        final List<BitSet> accounts = ClientsCurrencyAccountsGenerator.generateClients(1_000_000, allowedCurrencies, 1);

        //accounts.forEach(currencies -> log.debug("{}", currencies));

        allowedCurrencies.keySet().forEach(c1 -> {
            IntIntHashMap counters = new IntIntHashMap();

            accounts.stream().filter(a -> a.get(c1)).forEach(a -> {
                a.stream().forEach(c2 -> counters.addToValue(c2, 1));
            });

            log.debug("{}: {}", c1, counters);

        });


    }


    @Test
    public void shouldCreateClientsListForSymbol() {

        final Map<Integer, Double> allowedCurrencies = CurrenciesGenerator.randomCurrencies(100, 1, 1);

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

    @Test
    public void shouldGenerateAccountsForTransfers() {
        final Map<Integer, Double> allowedCurrencies = CurrenciesGenerator.randomCurrencies(31, 1, 1);

        ClientsCurrencyAccountsGenerator.generateAccountsForTransfers(
                100,
                allowedCurrencies,
                ClientsCurrencyAccountsGeneratorTest::mapToAccount,
                10,
                4143962);

    }

    private static long mapToAccount(long clientId, int currencyId, int accountNum) {

        if (clientId > 0x7_FFFF_FFFFL) {
            throw new IllegalArgumentException("clientId is too big");
        }

        if (currencyId > 0xFFFF) {
            throw new IllegalArgumentException("currencyId is too big");
        }

        if (accountNum > 0xFF) {
            throw new IllegalArgumentException("accountNum is too big");
        }

        final long accountRaw = (clientId << 28) | ((long) currencyId << 12) | ((long) accountNum << 4);
        final int checkDigit = Hashing.hash(accountRaw) & 0xF;
        log.debug("{} {} {} -> {} + CD={} -> {}", clientId, currencyId, accountNum, accountRaw, checkDigit, accountRaw | checkDigit);
        return accountRaw | checkDigit;
    }

}