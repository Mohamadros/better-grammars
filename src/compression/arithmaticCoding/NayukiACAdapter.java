package compression.arithmaticCoding;

import compression.arithmaticCoding.bigDecimalAc.Interval;
import compression.arithmaticCoding.nayukiAc.BitOutputStream;
import compression.arithmaticCoding.nayukiAc.NayukiArithmeticEncoderAdapter;

import java.io.IOException;
import java.util.List;

public final class NayukiACAdapter implements ArithmeticCodingEngine {

    private final NayukiArithmeticEncoderAdapter encoder;

    public NayukiACAdapter(BitOutputStream out) {
        this.encoder = new NayukiArithmeticEncoderAdapter(out);
    }

    @Override
    public void encodeNext(List<Interval> options, Interval chosen) throws IOException {
        encoder.encodeNext(options, chosen);
    }

    @Override
    public void finish() throws IOException {
        encoder.finish();
    }
}

