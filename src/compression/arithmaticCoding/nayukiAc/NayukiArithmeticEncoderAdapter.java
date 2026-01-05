package compression.arithmaticCoding.nayukiAc;

import compression.arithmaticCoding.bigDecimalAc.Interval;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

public final class NayukiArithmeticEncoderAdapter {

    private static final int STATE_BITS = 32;
    private static final int TOTAL_SCALE = 1 << 18;
    private static final MathContext MC = MathContext.DECIMAL128;

    private final ArithmeticEncoderNayuki encoder;

    public NayukiArithmeticEncoderAdapter(BitOutputStream out) {
        this.encoder = new ArithmeticEncoderNayuki(STATE_BITS, out);
    }

    public void encodeNext(List<Interval> options, Interval chosen) throws IOException {
        if (options.isEmpty()) {
            throw new IllegalArgumentException("Options list must not be empty");
        }

        int[] freqs = new int[options.size()];

        // 1) Compute raw weights from BigDecimal intervals
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

            if (w <= 0) w = 1;   // ensure non-zero frequency
            freqs[i] = w;
            sum += w;
        }

        // 2) Normalize to fixed TOTAL_SCALE
        int diff = TOTAL_SCALE - sum;
        freqs[0] += diff; // deterministic correction

        // 3) Build frequency table
        FrequencyTable table = new SimpleFrequencyTable(freqs);

        // 4) Find symbol index deterministically
        int symbol = indexOf(options, chosen);
        if (symbol < 0) {
            throw new IllegalArgumentException("Chosen interval not found in options");
        }

        encoder.write(table, symbol);
    }

    public void finish() throws IOException {
        encoder.finish();
    }

    private static FrequencyTable buildFrequencyTable(List<Interval> options) {
        int[] freqs = new int[options.size()];
        for (int i = 0; i < options.size(); i++) {
            BigDecimal len = options.get(i).getLength();
            int w = len.multiply(BigDecimal.valueOf(TOTAL_SCALE), MC).intValue();
            freqs[i] = Math.max(1, w);
        }
        return new SimpleFrequencyTable(freqs);
    }

    private static int indexOf(List<Interval> options, Interval chosen) {
        for (int i = 0; i < options.size(); i++) {
            Interval it = options.get(i);
            if (it.getLowerBound().compareTo(chosen.getLowerBound()) == 0 &&
                    it.getUpperBound().compareTo(chosen.getUpperBound()) == 0) {
                return i;
            }
        }
        return -1;
    }
}
