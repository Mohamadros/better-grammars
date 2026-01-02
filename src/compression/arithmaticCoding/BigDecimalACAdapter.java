package compression.arithmaticCoding;

import compression.arithmaticCoding.bigDecimalAc.ArithmeticEncoder;
import compression.arithmaticCoding.bigDecimalAc.ExactArithmeticEncoder;
import compression.arithmaticCoding.bigDecimalAc.Interval;

import java.io.IOException;
import java.util.List;

public final class BigDecimalACAdapter implements ArithmeticCodingEngine{
    private final ArithmeticEncoder encoder;

    public BigDecimalACAdapter() {
        this.encoder = new ExactArithmeticEncoder();
    }

    @Override
    public void encodeNext(List<Interval> options, Interval chosen) throws IOException {
        encoder.encodeNext(chosen);
    }

}