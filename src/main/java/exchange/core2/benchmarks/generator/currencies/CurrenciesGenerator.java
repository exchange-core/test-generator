package exchange.core2.benchmarks.generator.currencies;

import exchange.core2.benchmarks.generator.util.RandomUtils;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CurrenciesGenerator {

    public static Map<Integer, Double> randomCurrencies(final int num,
                                                        final int currencyIdShift,
                                                        final int seed) {

        final JDKRandomGenerator rand = new JDKRandomGenerator(seed);
        final double[] doubles = RandomUtils.paretoDistribution(num, rand);
//        Arrays.sort(doubles);
//        ArrayUtils.reverse(doubles);

        return IntStream.range(0, num)
                .boxed()
                .collect(Collectors.toMap(
                        i -> currencyIdShift + i,
                        i -> doubles[i]));
    }


    public static Map<Integer, Double> generateRandomRates(final Stream<Integer> currenciesStream,
                                                           final double power,
                                                           final int seed) {

        final RandomGenerator rng = new JDKRandomGenerator(seed);

        return currenciesStream.collect(
                Collectors.toMap(
                        c -> c,
                        c -> (double) (float) Math.exp(rng.nextGaussian() * power)));
    }

    public static Map<Integer, Map<Integer, Double>> createRatesMatrix(final Map<Integer, Double> rates) {

        final Map<Integer, Map<Integer, Double>> ratesMatrix = new HashMap<>();

        rates.forEach((currencyFrom, rateFrom) ->
                ratesMatrix.put(
                        currencyFrom,
                        rates.entrySet().stream()
                                .filter(e -> !currencyFrom.equals(e.getKey()))
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        e -> (e.getValue() / rateFrom)))));

        return ratesMatrix;
    }

}
