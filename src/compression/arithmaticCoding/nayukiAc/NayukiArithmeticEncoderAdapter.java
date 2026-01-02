package compression.arithmaticCoding.nayukiAc;

import compression.arithmaticCoding.bigDecimalAc.Interval;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;

import java.util.List;

public final class NayukiArithmeticEncoderAdapter {

    private static final int STATE_BITS = 32;
    private static final int SCALE = 1 << 20;

    private final ArithmeticEncoderNayuki encoder;

    public NayukiArithmeticEncoderAdapter(BitOutputStream out) {
        this.encoder = new ArithmeticEncoderNayuki(STATE_BITS, out);
    }

    public void encodeNext(List<Interval> options, Interval chosen) throws IOException {
        FrequencyTable freqs = buildFrequencyTable(options);
        int symbol = options.indexOf(chosen);
        if (symbol < 0)
            throw new IllegalArgumentException("Chosen interval not in options");
        encoder.write(freqs, symbol);
    }

    public void finish() throws IOException {
        encoder.finish();
    }

//    private static FrequencyTable buildFrequencyTable(List<Interval> options) {
//        int[] freqs = new int[options.size()];
//        for (int i = 0; i < options.size(); i++) {
////            double len = options.get(i).getLength().doubleValue();
////            freqs[i] = Math.max(1, (int) (len * SCALE));
//            BigDecimal len = options.get(i).getLength();
//            freqs[i] = Math.max(
//                    1,
//                    len.multiply(BigDecimal.valueOf(SCALE), MathContext.DECIMAL128).intValue()
//            );
//
//        }
//        return new SimpleFrequencyTable(freqs);
//    }

    private static FrequencyTable buildFrequencyTable(List<Interval> options) {
        int[] freqs = new int[options.size()];

        for (int i = 0; i < options.size(); i++) {
            BigDecimal len = options.get(i).getLength();

            int w = len
                    .multiply(BigDecimal.valueOf(SCALE), MathContext.DECIMAL128)
                    .intValue();

            if (w <= 0) w = 1;
            freqs[i] = w;

        }

        return new SimpleFrequencyTable(freqs);
    }

}



