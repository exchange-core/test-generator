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
package exchange.core2.benchmarks.generator.util;

import org.apache.commons.math3.distribution.ParetoDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;

public class RandomUtils {

    public static double[] paretoDistribution(final int size, final RandomGenerator rand) {
        final RealDistribution paretoDistribution = new ParetoDistribution(rand, 0.001, 1.5);
        final double[] paretoRaw = DoubleStream.generate(paretoDistribution::sample).limit(size).toArray();

        // normalize
        final double sum = Arrays.stream(paretoRaw).sum();
        double[] doubles = Arrays.stream(paretoRaw).map(x -> x / sum).toArray();
//        Arrays.stream(doubles).sorted().forEach(d -> log.debug("{}", d));
        return doubles;
    }

    public static <T extends Enum<T>> Supplier<T> enumValuesSupplier(final EnumSet<T> allowedValues,
                                                                     final Random random) {
        if (allowedValues.isEmpty()) {
            throw new IllegalArgumentException("allowedValues enumset can not be empty");
        }

        final List<T> list = new ArrayList<>(allowedValues);
        int size = list.size();
        if (size == 1) {
            final T first = list.get(0);
            return () -> first;
        } else {
            return () -> list.get(random.nextInt(size));
        }
    }
}
