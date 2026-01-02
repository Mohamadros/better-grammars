package compression.arithmaticCoding;

import compression.arithmaticCoding.nayukiAc.BitOutputStream;
public final class ArithmeticCodingFactory {

    public enum Backend {
        BIG_DECIMAL,
        NAYUKI
    }

    public static ArithmeticCodingEngine createEncoder(
            Backend backend,
            BitOutputStream out
    ) {
        switch (backend) {
            case NAYUKI:
                return new NayukiACAdapter(out);
            case BIG_DECIMAL:
            default:
                return new BigDecimalACAdapter();
        }
    }

    private ArithmeticCodingFactory() {}
}
