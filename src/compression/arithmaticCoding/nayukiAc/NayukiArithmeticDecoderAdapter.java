package compression.arithmaticCoding.nayukiAc;

import compression.arithmaticCoding.bigDecimalAc.ArithmeticDecoder;
import compression.arithmaticCoding.bigDecimalAc.Interval;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for Nayuki's arithmetic decoder that keeps the grammar-facing API unchanged.
 * <p>
 * The adapter accepts the bit-string output from  NayukiArithmeticEncoderAdapter,
 * feeds it into Nayuki's decoder, and maps each grammar interval to cumulative
 * integer counts using the same scaling strategy as the encoder:
 * {@code lowCount = floor(lower * totalFrequency)} and
 * {@code highCount = ceil(upper * totalFrequency)}. The resulting cumulative
 * counts are served through a lightweight {@code FrequencyTable} proxy so that
 * Nayuki's decoder can operate without touching the grammar code or the original
 * arithmetic coder implementation.
 * <p>
 * Nayuki's classes live in the unnamed package; reflection keeps this adapter
 * isolated from those sources while still wiring everything together.
 */
public class NayukiArithmeticDecoderAdapter implements ArithmeticDecoder {

    private static final int DEFAULT_STATE_BITS = 32;
    private static final int DEFAULT_TOTAL_FREQUENCY = 1 << 15;

    private final int totalFrequency;
    private final int stateBits;

    private final Object bitInputStream;
    private final Object arithmeticDecoder;

    private final Class<?> bitInputClass;
    private final Class<?> arithmeticDecoderClass;
    private final Class<?> frequencyTableClass;

    /**
     * Build a decoder for the given bit string using default parameters.
     */
    public NayukiArithmeticDecoderAdapter(String encodedBits) {
        this(encodedBits, DEFAULT_STATE_BITS, DEFAULT_TOTAL_FREQUENCY);
    }

    /**
     * Build a decoder with explicit Nayuki configuration.
     */
    public NayukiArithmeticDecoderAdapter(String encodedBits, int stateBits, int totalFrequency) {
        try {
            this.stateBits = stateBits;
            this.totalFrequency = totalFrequency;
            this.frequencyTableClass = Class.forName("FrequencyTable");
            this.bitInputClass = Class.forName("BitInputStream");
            this.arithmeticDecoderClass = Class.forName("ArithmeticDecoder");
            this.bitInputStream = instantiateBitInput(encodedBits);
            this.arithmeticDecoder = instantiateArithmeticDecoder();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize Nayuki decoder adapter", e);
        }
    }

    @Override
    public Interval decodeNext(List<Interval> options) {
        try {
            CumulativeCounts counts = toCounts(options);
            Object freqTable = frequencyTableProxy(counts.low, counts.high);
            Method read = arithmeticDecoderClass.getMethod("read", frequencyTableClass);
            int symbolIndex = (Integer) read.invoke(arithmeticDecoder, freqTable);
            return options.get(symbolIndex);
        } catch (Exception e) {
            throw new IllegalStateException("Decoding failed", e);
        }
    }

    private CumulativeCounts toCounts(List<Interval> intervals) {
        int size = intervals.size();
        List<Integer> lows = new ArrayList<>(size);
        List<Integer> highs = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Interval interval = intervals.get(i);
            int low = scale(interval.getLowerBound(), RoundingMode.FLOOR);
            int high = scale(interval.getUpperBound(), RoundingMode.CEILING);
            low = clamp(low, 0, totalFrequency);
            high = clamp(high, 0, totalFrequency);
            if (high <= low) {
                high = Math.min(totalFrequency, low + 1);
            }
            // Ensure the final cumulative endpoint aligns with the total to avoid gaps at the top.
            if (i == size - 1) {
                high = totalFrequency;
            }
            lows.add(low);
            highs.add(high);
        }
        return new CumulativeCounts(lows, highs);
    }

    private int scale(BigDecimal value, RoundingMode roundingMode) {
        return value.multiply(BigDecimal.valueOf(totalFrequency))
                .setScale(0, roundingMode)
                .intValue();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private Object frequencyTableProxy(List<Integer> lows, List<Integer> highs) {
        final int symbolLimit = lows.size();
        return Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{frequencyTableClass},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getSymbolLimit":
                            return symbolLimit;
                        case "get":
                            int idx = (Integer) args[0];
                            validateIndex(idx, symbolLimit);
                            return highs.get(idx) - lows.get(idx);
                        case "getTotal":
                            return totalFrequency;
                        case "getLow":
                            validateIndex((Integer) args[0], symbolLimit);
                            return lows.get((Integer) args[0]);
                        case "getHigh":
                            validateIndex((Integer) args[0], symbolLimit);
                            return highs.get((Integer) args[0]);
                        case "set":
                        case "increment":
                            throw new UnsupportedOperationException("Adapter frequency table is immutable");
                        default:
                            return method.invoke(proxy, args);
                    }
                });
    }

    private void validateIndex(int idx, int limit) {
        if (idx < 0 || idx >= limit) {
            throw new IllegalArgumentException("Symbol out of range");
        }
    }

    private Object instantiateBitInput(String encodedBits) throws Exception {
        byte[] encodedBytes = toBytes(encodedBits);
        Constructor<?> constructor = bitInputClass.getConstructor(java.io.InputStream.class);
        return constructor.newInstance(new ByteArrayInputStream(encodedBytes));
    }

    private Object instantiateArithmeticDecoder() throws Exception {
        Constructor<?> constructor = arithmeticDecoderClass.getConstructor(int.class, bitInputClass);
        return constructor.newInstance(stateBits, bitInputStream);
    }

    private byte[] toBytes(String bits) {
        int byteLength = (bits.length() + 7) / 8;
        byte[] data = new byte[byteLength];
        int bitIndex = 0;
        for (int i = 0; i < bits.length(); i++) {
            char c = bits.charAt(i);
            if (c != '0' && c != '1') {
                throw new IllegalArgumentException("Bit string must contain only '0' or '1'");
            }
            int bytePos = bitIndex / 8;
            int bitPos = 7 - (bitIndex % 8);
            if (c == '1') {
                data[bytePos] |= (1 << bitPos);
            }
            bitIndex++;
        }
        // Remaining bits (if any) are already zero for padding.
        return data;
    }

    private static final class CumulativeCounts {
        final List<Integer> low;
        final List<Integer> high;

        CumulativeCounts(List<Integer> low, List<Integer> high) {
            this.low = low;
            this.high = high;
        }
    }
}