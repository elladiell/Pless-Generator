import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class StatisticsTests {

    /**
     * Evaluates (period · C(t)) for 0 <= t < period
     *
     * @param bits - byte array with bits
     * @param ac   - results for all t
     */
    public static void autoCorrelationFunction(byte[] bits, int period, int[] ac) {
        Arrays.fill(ac, 0);
        for (int t = 0; t < period; t++) {
            for (int i = 0; i < period; i++) {
                int si = getBitFromByteArray(bits, i);
                int sit = getBitFromByteArray(bits, (i + t) % period);
                ac[t] += (2 * si - 1) * (2 * sit - 1);
            }
        }
    }


    private static int getBitFromByteArray(byte[] bits, int i) {
        int byteWithBitI = bits[i / 8];
        int bitI = (byteWithBitI >>> (7 - (i % 8))) & 0x01;
        return bitI;
    }

    private static int getBitBlockValue(byte[] bitsSequence, int startPos, int length) {
        if ((startPos + length) / 8 > bitsSequence.length) {
            throw new IllegalArgumentException("Index of bit is very big: " + startPos + " + " + length + " > " + bitsSequence.length * 8);
        }
        int byteWithBitI = bitsSequence[startPos / 8];
        // 1 1 1 1 0 1 0 1   биты в байте
        // 0 1 2 3 4 5 6 7   номер бита
        int bitNoInsideByteHighToLow = startPos % 8;
        int result = 0;
        for (int i = startPos; i < startPos + length; i++) {
            int bitI = (byteWithBitI >>> (7 - bitNoInsideByteHighToLow)) & 0x01;
            if (i > 0) {
                result <<= 1;
            }
            result |= bitI;
            if (++bitNoInsideByteHighToLow == 8) {
                bitNoInsideByteHighToLow = 0;
                ++byteWithBitI;
            }
        }
        return result;
    }

    public static int countBits(byte[] bits, int n, int value) {
        int count = 0;
        for (int i = 0; i < n; i++) {
            int si = getBitFromByteArray(bits, i);
            if (value == si) count++;
        }
        return count;
    }

    public static int countTwoBitsWithIntersection(byte[] bits, int n, int value) {
        int count = 0;
        for (int i = 0; i < n - 1; i++) {
            int si = getBitFromByteArray(bits, i);
            int siNext = getBitFromByteArray(bits, i + 1);
            int twoBits = (si << 1) | siNext;
            if (value == twoBits) count++;
        }
        return count;
    }

    /**
     * Пороговое значение -  3.8415 - для колич степеней свободы = 1
     *
     * @param seq
     * @param n
     * @return
     */
    public static double frequencyTest(byte[] seq, int n) {
        double n0 = countBits(seq, n, 0);
        double n1 = countBits(seq, n, 1);
        return (n0 - n1) * (n0 - n1) * 1.0 / n;
    }

    /**
     * Пороговое значение -  5.9915 - (для колич степеней свободы = 2)
     *
     * @param seq
     * @param n
     * @return
     */

    public static double twoBitsTest(byte[] seq, int n) {
        double n0 = countBits(seq, n, 0);
        double n1 = countBits(seq, n, 1);
        double n00 = countTwoBitsWithIntersection(seq, n, 0b00);
        double n01 = countTwoBitsWithIntersection(seq, n, 0b01);
        double n10 = countTwoBitsWithIntersection(seq, n, 0b10);
        double n11 = countTwoBitsWithIntersection(seq, n, 0b11);
        return 4.0 * (n00 * n00 + n01 * n01 + n10 * n10 + n11 * n11) / (n - 1) - 2.0 * (n0 * n0 + n1 * n1) / n + 1;
    }

    public static double pokerTest(byte[] bits, int n, int m) throws Exception {
        int k = n / m;
        int[] seriesCounts = new int[(int) Math.pow(2, m)];
        Arrays.fill(seriesCounts, 0);
        if (m > 32) throw new Exception("Could not count series with length > 32");
        for (int i = 0; i < n - n % m; ) {
            int subSeq = 0;

            for (int j = 0; j < m; ++j) {
                if (j != 0) {
                    subSeq <<= 1;
                }
                subSeq |= getBitFromByteArray(bits, i++);
            }
            ++seriesCounts[subSeq];
        }
        int sumOfSquares = 0;
        for (int i = 0; i < seriesCounts.length; i++) {
            sumOfSquares += seriesCounts[i] * seriesCounts[i];
        }
        return Math.pow(2, m) * sumOfSquares / k - k;
    }

    public static double seriesTest(byte[] bits, int n, int k) {
        Map<Integer, Integer> blocks = new HashMap<>();
        Map<Integer, Integer> gaps = new HashMap<>();
        int currentSeriesType = 0;
        int counter = 0;
        for (int i = 0; i < n; ++i) {
            int bit = getBitFromByteArray(bits, i);
            if (i == 0) {
                currentSeriesType = bit;
            }

            BiFunction<Integer, Integer, Integer> bi = (key, oldCount) -> oldCount == null ? 1 : ++oldCount;
            if (currentSeriesType == bit) {
                ++counter;
            } else {
                if (currentSeriesType == 1) {
                    blocks.compute(counter, bi);
                } else {
                    gaps.compute(counter, bi);
                }
                currentSeriesType = bit;
                counter = 1;
            }

            if (i == n - 1) {
                if (currentSeriesType == 1) {
                    blocks.compute(counter, bi);
                } else {
                    gaps.compute(counter, bi);
                }
            }
        }


        double sumBlocks = 0;
        double sumGaps = 0;
        for (int i = 1; i <= k; i++) {
            Integer bi = blocks.get(i);
            if (bi == null) bi = 0;
            Integer gi = gaps.get(i);
            if (gi == null) gi = 0;
            double e = evalE(i, n);
            sumBlocks += Math.pow(bi - e, 2) / e;
            sumGaps += Math.pow(gi - e, 2) / e;
        }
        return sumBlocks + sumGaps;

    }

    private static double evalE(int i, int n) {
        return (n - i + 3) / Math.pow(2, i + 2);
    }

    public static double autocorrelationTest(byte[] bits, int n) {
        int d = Math.max( n / 127 , 1);
        int a = 0;
        for (int i = 0; i < n - d; i++) {
            a += getBitFromByteArray(bits, i) ^ getBitFromByteArray(bits, i + d);
        }
        return 2. * (a - (n - d) / 2.) / Math.sqrt(n - d);
    }

    private static int findMaxM(int n) {
        int m = 1;
        for (int i = 2; i < n; i++) {
            if ((n * 1. / i )>= 5 * Math.pow(2, i)) {
                m = i;
            } else {
                break;
            }
        }
        return m;
    }


    private static double universalTest(byte[] bitSequence, int size, int L) {
        if (L < 6 || L > 16) throw new IllegalArgumentException("параметр L выбирается из интервала[6,16]");
        int pow2L = (int) Math.pow(2, L);
        if (size < 1010 * pow2L * L)
            throw new IllegalArgumentException("образец последовательности  bitSequence должен быть как минимум (1010 * 2^L * L) бит длиной");

        int blockCount = size / L;
        int Q = blockCount / 101;
        int K = blockCount - Q;
        int[] T = new int[pow2L]; //тут уже нули
        for (int i = 0; i < Q; i++) {
            int blockValue = getBitBlockValue(bitSequence, i * L, L);
            T[blockValue] = i;
        }
        double sum = 0;
        for (int i = Q; i < blockCount; i++) {
            int blockValue = getBitBlockValue(bitSequence, i * L, L);
            sum += Math.log10(i - T[blockValue]);
            T[blockValue] = i;
        }
        sum /= K;
        return sum;
    }


    public static void main(String[] args) throws Exception {

        byte[] subseq = {(byte) 0xe3, 0x11, 0x4e, (byte) 0xf2, 0x49};
        ByteBuffer buffer = ByteBuffer.allocate(20);
        buffer.put(subseq);
        buffer.put(subseq);
        buffer.put(subseq);
        buffer.put(subseq);
        byte[] bitSequence = buffer.array();
//        byte[] bitSequence = {0x64, 0x7a};
        int period = 160;
        runTests(bitSequence, 160);

    }

    /**
     * Для всех потоковых генераторов реализовать следующие статистические тесты:
     * <p>
     * 1. Частотный тест
     * 2. Последовательный тест
     * 3. Покер тест
     * 4. Тест серий
     * 5. Автокорреляционный тест
     * 6. Универсальный тест
     * <p>
     * По результатам проведенных тестов сделать выводы о качестве и стойкости реализованных систем шифрования
     */
    public static void runTests(byte[] bitSequence, int size) throws Exception {

        int countOfZeroes = countBits(bitSequence, size, 0);
        int countOfUnits = countBits(bitSequence, size, 1);
        System.out.println("countOfZeroes = " + countOfZeroes);
        System.out.println("countOfUnits = " + countOfUnits);

        double freqTest = frequencyTest(bitSequence, size);
        double twoBitsTest = twoBitsTest(bitSequence, size);


        double autocrlTest = autocorrelationTest(bitSequence, size);

        double alpha = 0.05; //уровень значимости тестов.
        System.out.println("freqTest = " + freqTest + " STATUS: " + (checkStatusForTest(freqTest, alpha, "x2", 1) ? "PASS" : "FAIL"));
        System.out.println("twoBitsTest = " + twoBitsTest + " STATUS: " + (checkStatusForTest(twoBitsTest, alpha, "x2", 2) ? "PASS" : "FAIL"));
//
//        int m = findMaxM(size);
//        double pokerTest = pokerTest(bitSequence, size, m);
//        System.out.println("pokerTest = " + pokerTest + " STATUS: " + (checkStatusForTest(pokerTest, alpha, "x2", (int) Math.pow(2, m) - 1) ? "PASS" : "FAIL"));
        int k = findMaxK(size);
        if (k > 1) {
            double seriesTest = seriesTest(bitSequence, size, k);
            System.out.println("seriesTest = " + seriesTest + " STATUS: " + (checkStatusForTest(seriesTest, alpha, "x2", 2 * k - 2) ? "PASS" : "FAIL"));
        } else {
            System.out.println("seriesTest: longest series must be > 1 ");
        }
        System.out.println("autocrlTest = " + autocrlTest + " STATUS: " + (checkStatusForTest(autocrlTest, alpha, "n", 0) ? "PASS" : "FAIL"));

        double universalTestVal = universalTest(bitSequence, size, 8);
        System.out.println("universalTestVal = " + universalTestVal + " STATUS: " + (checkStatusForTest(universalTestVal, 0.005, "n", 0) ? "PASS" : "FAIL"));
    }


    private static int findMaxK(int period) {
        int k = 1;
        double e;
        while ((e = evalE(k, period)) >= 5) {
            ++k;
        }
        if (e < 5) {
            k--;
        }
        if (k < 1) k = 1;
        return k;
    }

    private static boolean checkStatusForTest(double value, double alpha, String distribution, int v) throws Exception {
        //Хи квадрат распределение:
        if (distribution.equals("x2")) {
            if (alpha == 0.05) {
                double[] percentiles = {3.8415, 5.9915, 7.8147, 9.4877, 11.0705, 12.5916, 14.0671,
                        15.5073, 16.9190, 18.3070, 19.6751, 21.0261, 22.3620, 23.6848, 24.9958, 26.2962, 27.5871, 28.8693,
                        30.1435, 31.4104, 32.6706, 33.9244, 35.1725, 36.4150, 37.6525, 38.8851, 40.1133, 41.3371, 42.5570,
                        43.7730, 44.9853,
                };
                if (v >= percentiles.length) {
                    if(v <= 63) return  value <  82.5287;
                    if(v <= 127) return  value <  154.3015;
                    if(v <= 255) return  value <  293.2478;
                    if(v <= 511) return  value <  564.6961;
                    if(v <= 1023) return  value <  1098.5208;
                    throw new Exception("Процентили не известны для" + v);
                }
                return value < percentiles[v - 1];
            }
            throw new IllegalArgumentException("Процентили не известны");
        } else if (distribution.equals("n")) {
            //  alpha:                0.1    0.05    0.025    0.01   0.005  0.0025   0.001  0.0005
            double[] percentiles = {1.2816, 1.6449, 1.9600, 2.3263, 2.5758, 2.8070, 3.0902, 3.2905};
            double p = 0;
            if (alpha == 0.05) {
                p = percentiles[2];
            }
            if (alpha == 0.025) {
                p = percentiles[3];
            }
            if (alpha == 0.005) {
                p = percentiles[5];
            }
            return (value > -p) && (value < p);
        }
        throw new IllegalArgumentException("Процентили не известны");
    }
}
