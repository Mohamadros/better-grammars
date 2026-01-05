package compression.arithmaticCoding.nayukiAc;

import compression.arithmaticCoding.bigDecimalAc.Interval;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

/**
 * Decoder adapter corresponding to NayukiArithmeticEncoderAdapter.
 * Reconstructs the exact same integer frequency distribution
 * for each grammar expansion step.
 */
public final class NayukiArithmeticDecoderAdapter {

    private static final int STATE_BITS = 32;
    private static final int TOTAL_SCALE = 1 << 18;
    private static final MathContext MC = MathContext.DECIMAL128;

    private final ArithmeticDecoderNayuki decoder;

    public NayukiArithmeticDecoderAdapter(BitInputStream in) throws IOException {
        this.decoder = new ArithmeticDecoderNayuki(STATE_BITS, in);
    }

    /**
     * Decodes the next grammar choice.
     */
    public Interval decodeNext(List<Interval> options) throws IOException {
        if (options.isEmpty()) {
            throw new IllegalArgumentException("Options list must not be empty");
        }

        int[] freqs = buildNormalizedFrequencies(options);
        FrequencyTable table = new SimpleFrequencyTable(freqs);

        int symbol = decoder.read(table);
        return options.get(symbol);
    }

    /**
     * Builds a deterministic, normalized frequency distribution
     * identical to the encoder side.
     */
    private static int[] buildNormalizedFrequencies(List<Interval> options) {
        int[] freqs = new int[options.size()];

        BigDecimal totalLength = BigDecimal.ZERO;
        for (Interval it : options) {
            totalLength = totalLength.add(it.getLength(), MC);
        }

        if (totalLength.signum() <= 0) {
            throw new IllegalStateException("Total interval length must be positive");
        }

        int sum = 0;
        for (int i = 0; i < options.size(); i++) {
            BigDecimal len = options.get(i).getLength();

            int w = len
                    .multiply(BigDecimal.valueOf(TOTAL_SCALE), MC)
                    .divide(totalLength, MC)
                    .intValue();

            if (w <= 0) w = 1;
            freqs[i] = w;
            sum += w;
        }

        // Deterministic correction
        freqs[0] += (TOTAL_SCALE - sum);
        return freqs;
    }
}
