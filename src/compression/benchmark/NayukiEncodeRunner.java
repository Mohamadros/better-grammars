package compression.benchmark;

import compression.GenericRNAEncoder;
import compression.grammar.RNAWithStructure;
import compression.grammar.Rule;
import compression.arithmaticCoding.ArithmeticCodingEngine;
import compression.arithmaticCoding.ArithmeticCodingFactory;
import compression.arithmaticCoding.bigDecimalAc.Interval;
import compression.arithmaticCoding.nayukiAc.BitOutputStream;
import compression.samplegrammars.model.RuleProbModel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public final class NayukiEncodeRunner {

    public static byte[] encode(
            GenericRNAEncoder encoder,
            RNAWithStructure rna,
            RuleProbModel model
    ) throws IOException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        BitOutputStream bitOut = new BitOutputStream(bos);

        ArithmeticCodingEngine engine =
                ArithmeticCodingFactory.createEncoder(
                        ArithmeticCodingFactory.Backend.NAYUKI,
                        bitOut
                );

        for (Rule rule : encoder.leftmostDerivationFor(rna)) {
            List<Interval> options = model.getIntervalList(rule.left);
            Interval chosen = model.getIntervalFor(rule);

            engine.encodeNext(options, chosen);
        }

        engine.finish();
        bitOut.close();

        return bos.toByteArray();
    }

    private NayukiEncodeRunner() {}
}
