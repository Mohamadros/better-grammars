package compression.arithmaticCoding.nayukiAc;

import compression.arithmaticCoding.bigDecimalAc.Interval;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

public final class NayukiArithmeticDecoderAdapter {

    private static final int STATE_BITS = 32;
    private static final int SCALE = 1 << 20;

    private final ArithmeticDecoderNayuki decoder;

    public NayukiArithmeticDecoderAdapter(BitInputStream in) throws IOException {
        this.decoder = new ArithmeticDecoderNayuki(STATE_BITS, in);
    }

    public Interval decodeNext(List<Interval> options) throws IOException {
        FrequencyTable freqs = buildFrequencyTable(options);
        int symbol = decoder.read(freqs);
        return options.get(symbol);
    }

    private static FrequencyTable buildFrequencyTable(List<Interval> options) {
        int[] freqs = new int[options.size()];
        for (int i = 0; i < options.size(); i++) {
//            double len = options.get(i).getLength().doubleValue();
//            freqs[i] = Math.max(1, (int) (len * SCALE));
            BigDecimal len = options.get(i).getLength();
            freqs[i] = Math.max(
                    1,
                    len.multiply(BigDecimal.valueOf(SCALE), MathContext.DECIMAL128).intValue()
            );

        }
        return new SimpleFrequencyTable(freqs);
    }
}
