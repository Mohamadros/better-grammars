/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import compression.coding.ExactArithmeticEncoder;
import compression.grammargenerator.UnparsableException;
import compression.parser.CYKParser;
import compression.parser.Parser;
import compression.parser.SRFParser;
import compression.samplegrammars.model.StaticRuleProbModel;
import compression.grammar.NonTerminal;

import compression.grammar.*;
import compression.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import compression.samplegrammars.*;
import junit.framework.Assert;


public class GenericRNAEncoderTest {

    String pry = "G";
    String sec = ".";
    RNAWithStructure RNAWS;
    LiuGrammar Liu;
    Parser<PairOfChar> parser;
    NonTerminal S;
    
    public GenericRNAEncoderTest() {
        RNAWS = new RNAWithStructure(pry, sec);
        Liu= new LiuGrammar(false);
        parser = new SRFParser<>(Liu.getGrammar());
        S = new NonTerminal("S");
    }

    @Test
    public void testTraverseParseTree() throws UnparsableException {
        // split string into list of terminals (terminals)
        List<Terminal<PairOfChar>> terminals = RNAWS.asTerminals();
        List<Rule> der = parser.leftmostDerivationFor(terminals);
        Assert.assertEquals("[S → L, L → <G|.>]", der.toString());
    }

    @Test
    public void testencodeRNA() {

        System.out.println("Liu.getGrammar() = " + Liu.getGrammar());
        System.out.println("LiuProbs4Tests = " + new LiuProbs4Tests().LiuEtAlRuleProbs());
        GenericRNAEncoder GRA = new GenericRNAEncoder(
                new StaticRuleProbModel(Liu.getGrammar(),
                        new LiuProbs4Tests().LiuEtAlRuleProbs()),
                new ExactArithmeticEncoder(), Liu.getGrammar(), S);
        String encodedString = GRA.encodeRNA(RNAWS);
        Assert.assertEquals("111001", encodedString);
    }
}
