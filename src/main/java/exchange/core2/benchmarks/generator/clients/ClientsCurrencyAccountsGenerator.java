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

import exchange.core2.benchmarks.generator.util.ExecutionTime;
import exchange.core2.benchmarks.generator.GeneratorSymbolSpec;
import org.apache.commons.math3.distribution.ParetoDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class ClientsCurrencyAccountsGenerator {

    private static final Logger log = LoggerFactory.getLogger(ClientsCurrencyAccountsGenerator.class);

    /**
     * Generates random clients and different currencies they should have, so the total account is between
     * accountsToCreate and accountsToCreate+currencies.size()
     * <p>
     * In average each client will have account for 4 symbols (between 1 and currencies.size)
     *
     * @param accountsToCreate
     * @param currencies
     * @return n + 1 uid records with allowed currencies
     */
    public static List<BitSet> generateClients(final int accountsToCreate, Collection<Integer> currencies) {

        log.debug("Generating clients with {} accounts ({} currencies)...", accountsToCreate, currencies.size());

        final ExecutionTime executionTime = new ExecutionTime();
        final List<BitSet> result = new ArrayList<>();
        result.add(new BitSet()); // uid=0 no accounts

        final Random rand = new Random(1);

        final RealDistribution paretoDistribution = new ParetoDistribution(new JDKRandomGenerator(0), 1, 1.5);
        final int[] currencyCodes = currencies.stream().mapToInt(i -> i).toArray();

        int totalAccountsQuota = accountsToCreate;
        do {
            // TODO prefer some currencies more
            final int accountsToOpen = Math.min(Math.min(1 + (int) paretoDistribution.sample(), currencyCodes.length), totalAccountsQuota);
            final BitSet bitSet = new BitSet();
            do {
                final int currencyCode = currencyCodes[rand.nextInt(currencyCodes.length)];
                bitSet.set(currencyCode);
            } while (bitSet.cardinality() != accountsToOpen);

            totalAccountsQuota -= accountsToOpen;
            result.add(bitSet);

//            log.debug("{}", bitSet);

        } while (totalAccountsQuota > 0);

        log.debug("Generated {} clients with {} accounts up to {} different currencies in {}", result.size(), accountsToCreate, currencies.size(), executionTime.getTimeFormatted());
        return result;
    }

    public static int[] createClientsListForSymbol(final List<BitSet> clients2currencies, final GeneratorSymbolSpec spec, int symbolMessagesExpected) {

        // we would prefer to choose from same number of clients as number of messages to be generated in tests
        // at least 2 clients are required, but not more than half of all clients provided
        int numClientsToSelect = Math.min(clients2currencies.size(), Math.max(2, symbolMessagesExpected / 5));

        final ArrayList<Integer> uids = new ArrayList<>();
        final Random rand = new Random(spec.getSymbolId());
        int uid = 1 + rand.nextInt(clients2currencies.size() - 1);
        int c = 0;
        do {
            BitSet accounts = clients2currencies.get(uid);
            if (accounts.get(spec.getQuoteCurrency()) && (spec.getSymbolType() == GeneratorSymbolSpec.SymbolType.FUTURES_CONTRACT || accounts.get(spec.getBaseCurrency()))) {
                uids.add(uid);
            }
            if (++uid == clients2currencies.size()) {
                uid = 1;
            }
            //uid = 1 + rand.nextInt(clients2currencies.size() - 1);

            c++;
        } while (uids.size() < numClientsToSelect && c < clients2currencies.size());

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


}
