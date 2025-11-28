package compression.parser;

import compression.grammar.*;
import compression.grammargenerator.UnparsableException;
import compression.parser.CYKParser;
import compression.parser.SRFParser;
import compression.parser.StochasticParser;
import compression.samplegrammars.model.RuleProbModel;
import compression.samplegrammars.model.StaticRuleProbModel;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;

public class SRFParserTest extends TestCase {

        NonTerminal A0 = new NonTerminal("A0");
        NonTerminal A1 = new NonTerminal("A1");
        NonTerminal A2 = new NonTerminal("A2");
        NonTerminal A3 = new NonTerminal("A3");
        NonTerminal A4 = new NonTerminal("A4");


        CharTerminal OP = new CharTerminal('(');
        CharTerminal CL = new CharTerminal(')');
        CharTerminal DT = new CharTerminal('.');


        Grammar.Builder<Character> s= new Grammar.Builder<Character>("simpleGrammar",A0)
                .addRule(A0,DT)
                .addRule(A0, A0, A0)
                .addRule(A0, OP, A4, CL)
                .addRule(A1, DT)
                .addRule(A2, OP,A4,CL)
                .addRule(A3, DT)
                .addRule(A3,A1,A3)
                .addRule(A3, A2,A3)
                .addRule(A4, OP,A4,CL)
                .addRule(A4, A3)
                ;


        Grammar<Character> simpleGrammar= s.build();
        SRFParser<Character> testingSRFParser = new SRFParser<>(simpleGrammar);


        List<Terminal<Character>> word = new ArrayList<>(Arrays.asList(DT,OP,DT,CL));
        List<Terminal<Character>> word2 = new ArrayList<>(Arrays.asList(OP,OP,OP,DT,OP,DT,DT,DT,CL,OP,DT,DT,CL,CL,CL,CL,DT,DT,OP,DT,CL));




    @Test
    public void testsSRFParserLeftMostDerivation() throws Exception {
        List<Terminal<Character>> word = new ArrayList<>(Arrays.asList(DT,OP,DT,CL));
        assertEquals(testingSRFParser.mostLikelyLeftmostDerivationFor(word).toString(), "[A0 → A0 A0, A0 → ., A0 → ( A4 ), A4 → A3, A3 → .]");
    }

    @Test
    public void testSRFParserDerivationToWord() throws Exception {
        List<Terminal<Character>> word = new ArrayList<>(Arrays.asList(DT,OP,OP,DT,CL,CL,DT));
        SRFParser<Character> testingSRFParser2 = new SRFParser<>(simpleGrammar);
        System.out.println("1: "+word);
        System.out.println("2: "+testingSRFParser2.mostLikelyLeftmostDerivationFor(word));
        System.out.println("3: "+ testingSRFParser2.mostLikelyWord(word));
        assertEquals(testingSRFParser2.mostLikelyWord(word), word);
    }

    @Test
    public void testSRFParsable() {
        List<Terminal<Character>> word3 = new ArrayList<>(Arrays.asList(OP,DT,CL));
        SRFParser<Character> testingSRFParser3 = new SRFParser<>(simpleGrammar);
        //System.out.println("3: "+ testingSRFParser3.parsable(word3));
        assertEquals((testingSRFParser3.parsable(word3)),true);
    }

    @Test
    public void testMostLikely() throws UnparsableException {
        // Create ambiguous grammar and skewed rule prob model
        NonTerminal S = new NonTerminal("S");
        NonTerminal A = new NonTerminal("A");
        NonTerminal B = new NonTerminal("B");
        NonTerminal C = new NonTerminal("C");
        CharTerminal a = new CharTerminal('a');
        CharTerminal b = new CharTerminal('b');
        Rule rSA = new Rule(S, A);
        Rule rSB = new Rule(S, B);
        Rule rSC = new Rule(S, C);
        Rule rAa = new Rule(A, a);
        Rule rBa = new Rule(B, a);
        Rule rCa = new Rule(C, a);
        Rule rAb = new Rule(A, b);
        Rule rBb = new Rule(B, b);

        Grammar.Builder<Character> s = new Grammar.Builder<Character>("ambiguous", S)
                .addRules(List.of(rSA, rSB, rSC, rAa, rBa, rCa, rAb, rBb));
        Grammar<Character> G = s.build();
        RuleProbModel P = new StaticRuleProbModel(G, Map.of(
                rSA, 0.5,
                rSB, 0.3,
                rSC, 0.2,
                rAa, 0.1,
                rAb, 0.9,
                rBa, 0.5,
                rBb, 0.5,
                rCa, 1.0
                ));
        // S => A => a has prob 0.5 * 0.1 = 0.05
        // S => B => a has prob 0.3 * 0.5 = 0.15
        // S => C => a has prob 0.2 * 1.0 = 0.2
        // S => A => b has prob 0.5 * 0.9 = 0.45
        // S => B => b has prob 0.3 * 0.5 = 0.15
        StochasticParser<Character> parser = new SRFParser<>(G, P);
        Assert.assertEquals(Math.log(0.2), parser.logProbabilityOf(List.of(a)),0.0001);
        Assert.assertEquals(List.of(rSC, rCa), parser.mostLikelyLeftmostDerivationFor(List.of(a)));
        Assert.assertEquals(Math.log(0.45), parser.logProbabilityOf(List.of(b)),0.0001);
        Assert.assertEquals(List.of(rSA, rAb), parser.mostLikelyLeftmostDerivationFor(List.of(b)));
    }


}
