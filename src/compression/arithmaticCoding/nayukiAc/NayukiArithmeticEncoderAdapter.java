package compression.arithmaticCoding.nayukiAc;


import compression.arithmaticCoding.bigDecimalAc.ArithmeticEncoder;
import compression.arithmaticCoding.bigDecimalAc.Interval;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Adapter that lets the existing grammar code talk to Nayuki's arithmetic encoder
 * without changing any of the Nayuki sources.
 * <p>
 * The adapter keeps the existing {@link Interval}-based API untouched while mapping
 * every interval to integer cumulative frequencies understood by Nayuki's encoder.
 * The conversion is done by scaling the interval bounds to a fixed {@code totalFrequency}
 * (default: {@code 1 << 15}). For an interval {@code [low, high)} we compute
 * {@code lowCount = floor(low * totalFrequency)} and {@code highCount = ceil(high * totalFrequency)}.
 * These cumulative counts are then exposed through a tiny {@code FrequencyTable} proxy
 * with three symbols: the prefix mass {@code [0, lowCount)}, the actual rule
 * {@code [lowCount, highCount)}, and the remaining tail {@code [highCount, totalFrequency)}.
 * <p>
 * Note: Nayuki's reference implementation lives in the unnamed package, so this adapter
 * talks to it purely via reflection to keep the upstream sources untouched.
 */
public class NayukiArithmeticEncoderAdapter implements ArithmeticEncoder {

    private static final int DEFAULT_STATE_BITS = 32;
    private static final int DEFAULT_TOTAL_FREQUENCY = 1 << 15;

    private final int totalFrequency;
    private final int stateBits;

    private final ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
    private final Object bitOutputStream;
    private final Object arithmeticEncoder;

    private final Class<?> bitOutputClass;
    private final Class<?> arithmeticEncoderClass;
    private final Class<?> frequencyTableClass;

    private boolean finished = false;
    private String cachedEncoding = null;
    private int cachedPrecision = -1;

    /**
     * Uses the default state width (32 bits) and total frequency (1 &lt;&lt; 15).
     */
    public NayukiArithmeticEncoderAdapter() {
        this(DEFAULT_STATE_BITS, DEFAULT_TOTAL_FREQUENCY);
    }

    /**
     * Allows configuring the Nayuki coder parameters while keeping the grammar-side
     * interface stable.
     */
    public NayukiArithmeticEncoderAdapter(int stateBits, int totalFrequency) {
        try {
            this.stateBits = stateBits;
            this.totalFrequency = totalFrequency;
            this.frequencyTableClass = Class.forName("FrequencyTable");
            this.bitOutputClass = Class.forName("BitOutputStream");
            this.arithmeticEncoderClass = Class.forName("ArithmeticEncoder");
            this.bitOutputStream = instantiateBitOutput();
            this.arithmeticEncoder = instantiateArithmeticEncoder();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize Nayuki adapter", e);
        }
    }

    @Override
    public void encodeNext(Interval interval) {
        try {
            Counts counts = toCounts(interval);
            Object freqTable = threeSymbolFrequencyTable(counts.low, counts.high);
            Method write = arithmeticEncoderClass.getMethod("write", frequencyTableClass, int.class);
            // The middle symbol (index 1) represents the actual grammar rule.
            write.invoke(arithmeticEncoder, freqTable, 1);
        } catch (Exception e) {
            throw new IllegalStateException("Encoding failed", e);
        }
    }

    @Override
    public String getFinalEncoding() {
        if (cachedEncoding != null) {
            return cachedEncoding;
        }
        try {
            finishIfNecessary();
            cachedPrecision = computeBitCount();
            closeBitOutput();
            cachedEncoding = toBitString(byteOutput.toByteArray(), cachedPrecision);
            return cachedEncoding;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to finalize encoding", e);
        }
    }

    @Override
    public int getFinalPrecision() {
        if (cachedPrecision >= 0) {
            return cachedPrecision;
        }
        // Trigger finalization to compute precision.
        getFinalEncoding();
        return cachedPrecision;
    }

    /**
     * Converts a grammar interval into cumulative integer counts using the fixed
     * {@code totalFrequency}. The same conversion is reused by the decoder adapter
     * to guarantee symmetry.
     */
    private Counts toCounts(Interval interval) {
        BigDecimal lower = interval.getLowerBound();
        BigDecimal upper = interval.getUpperBound();
        int low = scale(lower, RoundingMode.FLOOR);
        int high = scale(upper, RoundingMode.CEILING);
        low = clamp(low, 0, totalFrequency);
        high = clamp(high, 0, totalFrequency);
        if (high <= low) {
            high = Math.min(totalFrequency, low + 1);
        }
        return new Counts(low, high);
    }

    private int scale(BigDecimal value, RoundingMode roundingMode) {
        return value.multiply(BigDecimal.valueOf(totalFrequency))
                .setScale(0, roundingMode)
                .intValue();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private Object threeSymbolFrequencyTable(int low, int high) {
        final int symbolFreq = Math.max(1, high - low);
        final int prefixFreq = Math.max(0, low);
        final int tailFreq = Math.max(0, totalFrequency - (prefixFreq + symbolFreq));

        final int[] cumulative = new int[]{
                0,
                prefixFreq,
                prefixFreq + symbolFreq,
                prefixFreq + symbolFreq + tailFreq
        };

        return Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{frequencyTableClass},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getSymbolLimit":
                            return 3;
                        case "get":
                            int idx = (Integer) args[0];
                            if (idx == 0) return prefixFreq;
                            if (idx == 1) return symbolFreq;
                            if (idx == 2) return tailFreq;
                            throw new IllegalArgumentException("symbol out of range");
                        case "getTotal":
                            return totalFrequency;
                        case "getLow":
                            return cumulative[(Integer) args[0]];
                        case "getHigh":
                            return cumulative[(Integer) args[0] + 1];
                        case "set":
                        case "increment":
                            throw new UnsupportedOperationException("Adapter frequency table is immutable");
                        default:
                            return method.invoke(proxy, args);
                    }
                });
    }

    private Object instantiateBitOutput() throws Exception {
        Constructor<?> constructor = bitOutputClass.getConstructor(java.io.OutputStream.class);
        return constructor.newInstance(byteOutput);
    }

    private Object instantiateArithmeticEncoder() throws Exception {
        Constructor<?> constructor = arithmeticEncoderClass.getConstructor(int.class, bitOutputClass);
        return constructor.newInstance(stateBits, bitOutputStream);
    }

    private void finishIfNecessary() throws Exception {
        if (finished) return;
        Method finish = arithmeticEncoderClass.getMethod("finish");
        finish.invoke(arithmeticEncoder);
        finished = true;
    }

    private void closeBitOutput() throws Exception {
        Method close = bitOutputClass.getMethod("close");
        close.invoke(bitOutputStream);
    }

    private int computeBitCount() throws Exception {
        Field numBitsField = bitOutputClass.getDeclaredField("numBitsFilled");
        numBitsField.setAccessible(true);
        int buffered = (Integer) numBitsField.get(bitOutputStream);
        return byteOutput.size() * 8 + buffered;
    }

    private String toBitString(byte[] data, int usefulBits) {
        StringBuilder sb = new StringBuilder(usefulBits);
        int bitsEmitted = 0;
        for (byte b : data) {
            for (int i = 7; i >= 0 && bitsEmitted < usefulBits; i--) {
                sb.append(((b >>> i) & 1) == 1 ? '1' : '0');
                bitsEmitted++;
            }
        }
        return sb.toString();
    }

    private static final class Counts {
        final int low;
        final int high;

        Counts(int low, int high) {
            this.low = low;
            this.high = high;
        }
    }
}