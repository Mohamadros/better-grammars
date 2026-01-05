package compression.benchmark;

import compression.arithmaticCoding.bigDecimalAc.Interval;
import compression.arithmaticCoding.nayukiAc.BitInputStream;
import compression.arithmaticCoding.nayukiAc.NayukiArithmeticDecoderAdapter;
import compression.grammar.*;
import compression.samplegrammars.model.RuleProbModel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public final class NayukiDecodeRunner {

    public static RNAWithStructure decode(
            byte[] encoded,
            RuleProbModel model,
            NonTerminal startSymbol
    ) throws IOException {

        BitInputStream bitIn = new BitInputStream(new ByteArrayInputStream(encoded));
        NayukiArithmeticDecoderAdapter dec = new NayukiArithmeticDecoderAdapter(bitIn);

        final List<Category> leftmostDerivation = new LinkedList<>();
        NonTerminal leftmostNT = startSymbol;
        leftmostDerivation.add(leftmostNT);

        while (leftmostNT != null) {
            final List<Interval> options = model.getIntervalList(leftmostNT);
            Interval interval = dec.decodeNext(options);

            List<Category> rhs = model.getRhsFor(interval, leftmostNT);
            leftmostNT = replaceFirstNonterminal(leftmostDerivation, rhs);
        }

        return getRNAString(decodeCategoryList(leftmostDerivation));
    }


    private static NonTerminal replaceFirstNonterminal(final List<Category> leftmostDerivation, List<Category> rhs) {
        for (ListIterator<Category> iterator = leftmostDerivation.listIterator(); iterator.hasNext(); ) {
            final Category cat = iterator.next();
            if (Category.isNonTerminal(cat)) {
                iterator.remove();
                for (Category category : rhs) iterator.add(category);

                for (Category category : rhs) iterator.previous();
                while (iterator.hasNext()) {
                    final Category nextCat = iterator.next();
                    if (Category.isNonTerminal(nextCat)) {
                        return (NonTerminal) nextCat;
                    }
                }
            }
        }
        return null;
    }

    private static java.util.ArrayList<PairOfChar> decodeCategoryList(List<Category> catList) {
        java.util.ArrayList<PairOfChar> pairOfCharList = new java.util.ArrayList<>();
        for (Category cat : catList) {
            if (!Category.isTerminal(cat))
                throw new IllegalArgumentException("only terminals allowed here");
            pairOfCharList.add(((PairOfCharTerminal) cat).getChars());
        }
        return pairOfCharList;
    }

    private static RNAWithStructure getRNAString(java.util.ArrayList<PairOfChar> POCList) {
        StringBuilder primary = new StringBuilder(POCList.size()),
                secondary = new StringBuilder(POCList.size());
        for (PairOfChar POC : POCList) {
            primary.append(POC.getPry());
            secondary.append(POC.getSec());
        }
        return new RNAWithStructure(primary.toString(), secondary.toString());
    }

    private NayukiDecodeRunner() {}
}
