/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import compression.grammar.RNAGrammar;
import compression.grammar.Rule;
import compression.samplegrammars.RuleCountsForGrammarLaPlace;
import compression.samplegrammars.model.AdaptiveRuleProbModel;
import compression.arithmaticCoding.bigDecimalAc.ArithmeticEncoder;
import compression.arithmaticCoding.bigDecimalAc.ExactArithmeticDecoder;
import compression.arithmaticCoding.bigDecimalAc.ExactArithmeticEncoder;
import compression.GenericRNADecoder;
import compression.GenericRNAEncoder;
import compression.samplegrammars.model.RuleProbModel;
import compression.samplegrammars.model.SemiAdaptiveRuleProbModel;
import compression.samplegrammars.model.StaticRuleProbModel;
import compression.data.TrainingDataset;
import compression.grammar.RNAWithStructure;
import compression.samplegrammars.SampleGrammar;
import junit.framework.Assert;
//import org.junit.Assert;

import java.io.IOException;
import java.util.Map;

public class SampleInstance4Tests {

    SampleGrammar G;


    public SampleInstance4Tests(SampleGrammar NewG) {

        G = NewG;

    }

    public void runEncodeNDecodeStatic(RNAWithStructure rnaws, TrainingDataset tDataset) throws IOException {
        //Encoding
        ArithmeticEncoder AE = new ExactArithmeticEncoder();
        RuleProbModel RPMStatic = new StaticRuleProbModel(G.getGrammar(), G.readRuleProbs(tDataset.ruleProbsFileFor(G)));
        GenericRNAEncoder GRAStatic = new GenericRNAEncoder(RPMStatic, AE, G.getGrammar(), G.getStartSymbol());
        String encodedStringStatic = GRAStatic.encodeRNA(rnaws);

        //Decoding
        ExactArithmeticDecoder AD = new ExactArithmeticDecoder(encodedStringStatic);
        GenericRNADecoder GRAD = new GenericRNADecoder(RPMStatic, AD, G.getStartSymbol());
        RNAWithStructure decoded = GRAD.decode();

        Assert.assertEquals(rnaws, decoded);
    }

    public void generateStaticModel(TrainingDataset tDataset) throws IOException {
        RNAGrammar rnaG = G.getGrammar();
        Map<Rule, Long> ruleCounts = new RuleCountsForGrammarLaPlace(rnaG, tDataset).ruleCounts();
        Map<Rule, Double> ruleProbs = RuleProbModel.computeRuleProbs(rnaG, ruleCounts);
        RuleProbModel staticModel = new StaticRuleProbModel(rnaG, ruleProbs);
        G.writeRuleProbs(tDataset.ruleProbsFileFor(G), ruleProbs);
    }

    public void runEncodeNDecode4Adaptive(RNAWithStructure rnaws) {
        //Encoding
        ArithmeticEncoder AE = new ExactArithmeticEncoder();
        RuleProbModel RPMAdaptive = new AdaptiveRuleProbModel(G.getGrammar());
        GenericRNAEncoder GRAdaptive = new GenericRNAEncoder(RPMAdaptive, AE, G.getGrammar(), G.getStartSymbol());
        String encodedStringAdaptive = GRAdaptive.encodeRNA(rnaws);

        //Decoding
        RPMAdaptive = new AdaptiveRuleProbModel(G.getGrammar());//resets the Model
        ExactArithmeticDecoder AD = new ExactArithmeticDecoder(encodedStringAdaptive);
        GenericRNADecoder GRAD = new GenericRNADecoder(RPMAdaptive, AD, G.getStartSymbol());
        RNAWithStructure decoded = GRAD.decode();

        Assert.assertEquals(rnaws, decoded);
    }
    public void runEncodeNDecode4SemiAdaptive(RNAWithStructure rnaws){
        //Encoding
        ArithmeticEncoder AE = new ExactArithmeticEncoder();
        RuleProbModel RPMSemiAdaptive = new SemiAdaptiveRuleProbModel(G.getGrammar(), rnaws);
        GenericRNAEncoder GRASemiAdaptive = new GenericRNAEncoder(RPMSemiAdaptive, AE, G.getGrammar(), G.getStartSymbol());
        String encodedStringStatic = GRASemiAdaptive.encodeRNA(rnaws);

        //Decoding
        ExactArithmeticDecoder AD = new ExactArithmeticDecoder(encodedStringStatic);
        GenericRNADecoder GRAD = new GenericRNADecoder(RPMSemiAdaptive, AD, G.getStartSymbol());
        RNAWithStructure decoded = GRAD.decode();

        Assert.assertEquals(rnaws, decoded);

    }

}
