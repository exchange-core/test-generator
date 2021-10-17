/*
 * Copyright 2018-2021 Maksim Zheravin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package exchange.core2.benchmarks.generator.clients;

import exchange.core2.benchmarks.generator.GeneratorSymbolSpec;
import exchange.core2.benchmarks.generator.util.ExecutionTime;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.distribution.ParetoDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public final class ClientsCurrencyAccountsGenerator {

    private static final Logger log = LoggerFactory.getLogger(ClientsCurrencyAccountsGenerator.class);

    /**
     * Generates random clients and different currency accounts they have
     * Total accounts number is between accountsToCreate and accountsToCreate+currencies.size()
     * <p>
     * In average each client will have account for 4 symbols (between 1 and currencies.size)
     * uid is reserved, so first entry is always empty
     * <p>
     * TODO use currencies indexes everywhere (as more friendly to bitsets)
     *
     * @param accountsToCreate  - number account to create (not clients)
     * @param currenciesWeights - weights of currencies (required for optimal distribution of accounts)
     * @param seed              - random seed
     * @return n + 1 uid records with allowed currencies
     */
    public static List<BitSet> generateClients(final int accountsToCreate,
                                               final Map<Integer, Double> currenciesWeights,
                                               int seed) {

        log.debug("Generating clients with {} accounts ({} currencies)...", accountsToCreate, currenciesWeights.size());

        final ExecutionTime executionTime = new ExecutionTime();
        final List<BitSet> result = new ArrayList<>();
        result.add(new BitSet()); // uid=0 no accounts

        final RandomGenerator rng = new JDKRandomGenerator(seed);

        final RealDistribution paretoDistribution = new ParetoDistribution(new JDKRandomGenerator(0), 1, 1.5);

        final int[] allCurrencies = currenciesWeights.keySet().stream().mapToInt(a -> a).toArray();

        // prepare distribution for currencies
        final List<Pair<Integer, Double>> currencyWeightPairs = currenciesWeights.entrySet().stream()
                .map(e -> Pair.create(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        final EnumeratedDistribution<Integer> currenciesDistribution = new EnumeratedDistribution<>(rng, currencyWeightPairs);

        // prepare reversed distribution for multi-currencies accounts (just an optimization)
        final List<Pair<Integer, Double>> currencyWeightPairsRev = currencyWeightPairs.stream()
                .map(p -> Pair.create(p.getFirst(), 1.0 / p.getSecond()))
                .collect(Collectors.toList());

        final EnumeratedDistribution<Integer> currenciesDistributionRev = new EnumeratedDistribution<>(rng, currencyWeightPairsRev);

        int totalAccountsQuota = accountsToCreate;
        do {

            final BitSet bitSet = generateClientAccounts(allCurrencies, paretoDistribution, currenciesDistribution, currenciesDistributionRev);

            totalAccountsQuota -= bitSet.cardinality();
            result.add(bitSet);

//            log.debug("{}", bitSet);

        } while (totalAccountsQuota > 0);

        log.debug("Generated {} clients with {} accounts up to {} different currencies in {}",
                result.size(), accountsToCreate, currenciesWeights.size(), executionTime.getTimeFormatted());

        return result;
    }

    private static BitSet generateClientAccounts(int[] allCurrencies,
                                                 RealDistribution accountsNumDistribution,
                                                 EnumeratedDistribution<Integer> currenciesDistribution,
                                                 EnumeratedDistribution<Integer> currenciesDistributionRev) {

        final int accountsToOpen = Math.min(
                1 + (int) accountsNumDistribution.sample(),
                allCurrencies.length);

        if (accountsToOpen < allCurrencies.length / 2) {
            final BitSet bitSet = new BitSet();

            // at least 1 account open.
            do {
                final int currencyCode = currenciesDistribution.sample();
                bitSet.set(currencyCode);
            } while (bitSet.cardinality() != accountsToOpen);
            return bitSet;

        } else {

            final BitSet bitSet = new BitSet();
            Arrays.stream(allCurrencies).forEach(bitSet::set);

            while (bitSet.cardinality() != accountsToOpen) {
                final int currencyCode = currenciesDistributionRev.sample();
                bitSet.clear(currencyCode);
            }
            return bitSet;
        }
    }

    public static int[] createClientsListForSymbol(final List<BitSet> clients2currencies,
                                                   final GeneratorSymbolSpec spec,
                                                   int symbolMessagesExpected,
                                                   int seed) {

        // we would prefer to choose from same number of clients as number of messages to be generated in tests
        // at least 2 clients are required, but not more than all clients provided
        final int numClientsToSelect = Math.min(clients2currencies.size(), Math.max(2, symbolMessagesExpected / 5));

        final ArrayList<Integer> uids = new ArrayList<>(numClientsToSelect);
        final Random rand = new Random(seed + spec.getSymbolId() * 17276);

        // for faster generation - chose one client randomly and then try all subsequent clients
        int uid = 1 + rand.nextInt(clients2currencies.size() - 1);
        int c = 0;
        do {
            BitSet accounts = clients2currencies.get(uid);

            // for exchange such algorithm tend to select clients having many accounts, such distribution looks realistic
            // TODO ? select more clients, sort and chose some having less number accounts
            if (accounts.get(spec.getQuoteCurrency()) && (spec.getSymbolType() == GeneratorSymbolSpec.SymbolType.FUTURES_CONTRACT || accounts.get(spec.getBaseCurrency()))) {
                uids.add(uid);
            }
            if (++uid == clients2currencies.size()) {
                uid = 1;
            }
            //uid = 1 + rand.nextInt(clients2currencies.size() - 1);

            if (c++ > clients2currencies.size()) {
                // tried every account, can stop
                // log.warn("WRAP");
                break;
            }

        } while (uids.size() < numClientsToSelect);

//        int expectedClients = symbolMessagesExpected / 20000;
//        if (uids.size() < Math.max(2, expectedClients)) {
//            // less than 2 uids
//            throw new IllegalStateException("Insufficient accounts density - can not find more than " + uids.size() + " matching clients for symbol " + spec.symbolId
//                    + " total clients:" + clients2currencies.size()
//                    + " symbolMessagesExpected=" + symbolMessagesExpected
//                    + " numClientsToSelect=" + numClientsToSelect);
//        }

//        log.debug("sym: " + spec.symbolId + " " + spec.type + " uids:" + uids.size() + " msg=" + symbolMessagesExpected + " numClientsToSelect=" + numClientsToSelect + " c=" + c);

        return uids.stream().mapToInt(x -> x).toArray();
    }

    public interface AccountEncoder {
        long encode(long clientId, int currencyId, int accountNum);
    }


    public static long[] generateAccountsForTransfers(final int numAccountsToCreate,
                                                      final Map<Integer, Double> currenciesWeights,
                                                      final AccountEncoder accountEncoder,
                                                      int maxAccountsPerClient,
                                                      int seed) {

        log.debug("Generating clients with {} accounts ({} currencies)...", numAccountsToCreate, currenciesWeights.size());

        final long[] accounts = new long[numAccountsToCreate];

        final ExecutionTime executionTime = new ExecutionTime();

        final RandomGenerator rng = new JDKRandomGenerator(seed);

        final RealDistribution accountsNumDistribution = new ParetoDistribution(new JDKRandomGenerator(0), 1, 1.5);

        // prepare distribution for currencies
        final List<Pair<Integer, Double>> currencyWeightPairs = currenciesWeights.entrySet().stream()
                .map(e -> Pair.create(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        final EnumeratedDistribution<Integer> currenciesDistribution = new EnumeratedDistribution<>(rng, currencyWeightPairs);

        int clientCounter = 0;
        int accountCounter = 0;
        int clientAccountLeft = 0;
        for (int i = 0; i < numAccountsToCreate; i++) {
            if (clientAccountLeft == 0) {
                clientAccountLeft = Math.min(1 + (int) accountsNumDistribution.sample(), maxAccountsPerClient);
                clientCounter++;
                accountCounter = 0;
            }
            final int currencyCode = currenciesDistribution.sample();
            accounts[i] = accountEncoder.encode(clientCounter, currencyCode, accountCounter);
            accountCounter++;
            clientAccountLeft--;
        }

        log.debug("Generated {} clients with {} accounts up to {} different currencies in {}",
                clientCounter, numAccountsToCreate, currenciesWeights.size(), executionTime.getTimeFormatted());

        return accounts;
    }

}
