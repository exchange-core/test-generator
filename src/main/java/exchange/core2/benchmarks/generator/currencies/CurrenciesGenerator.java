package exchange.core2.benchmarks.generator.currencies;

import exchange.core2.benchmarks.generator.util.RandomUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.random.JDKRandomGenerator;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CurrenciesGenerator {

    public static Map<Integer, Double> randomSymbols(final int num,
                                                     final int currencyIdShift,
                                                     final int seed) {
        final JDKRandomGenerator rand = new JDKRandomGenerator(seed);
        final double[] doubles = RandomUtils.weightedDistribution(num, rand);
//        Arrays.sort(doubles);
//        ArrayUtils.reverse(doubles);

        return IntStream.range(0, num)
                .boxed()
                .collect(Collectors.toMap(
                        i -> currencyIdShift + i,
                        i -> doubles[i]));
    }
}
