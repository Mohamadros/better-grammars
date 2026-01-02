package compression.arithmaticCoding;


import compression.arithmaticCoding.bigDecimalAc.Interval;
import java.io.IOException;
import java.util.List;

public interface ArithmeticCodingEngine {

    void encodeNext(List<Interval> options, Interval chosen) throws IOException;

    default void finish() throws IOException {
        // BigDecimal AC does nothing
    }
}

