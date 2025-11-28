package compression.samplegrammars;

import compression.grammar.CharTerminal;
import compression.grammar.Grammar;
import compression.grammar.NonTerminal;
import compression.grammar.PairOfChar;
import compression.grammar.PairOfCharTerminal;
import compression.grammar.RNAGrammar;
import compression.grammar.RNAWithStructure;
import compression.grammar.Rule;
import compression.grammar.SecondaryStructureGrammar;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

/**
 * @author Sebastian Wild (wild@liverpool.ac.uk)
 */
public class RuleProbsForGrammarSemiAdaptiveTest extends TestCase {

	@Test
	public void testRuleProbsForGrammarSemiAdaptive() {
		NonTerminal S = new NonTerminal("S");
		NonTerminal A = new NonTerminal("A");
		NonTerminal B = new NonTerminal("B");
		NonTerminal C = new NonTerminal("C");
		CharTerminal u = new CharTerminal('.');
		CharTerminal o = new CharTerminal('(');
		CharTerminal c = new CharTerminal(')');
		Rule rSA = new Rule(S, A);
		Rule rSB = new Rule(S, B);
		Rule rSC = new Rule(S, C);
		Rule rAu = new Rule(A, u);
		Rule rBu = new Rule(B, u);
		Rule rBoCc = new Rule(B, o, B, c);
		Rule rCu = new Rule(C, u);

		Grammar.Builder<Character> s = new Grammar.Builder<Character>("ambiguous", S)
				.addRules(List.of(rSA, rSB, rSC, rAu, rBu, rCu, rBoCc));
		SecondaryStructureGrammar ssg = SecondaryStructureGrammar.fromCheap(s.build());
		RNAGrammar G = RNAGrammar.from(ssg.convertToSRF(), false);
//		System.out.println("G = " + G);

		RNAWithStructure rna = new RNAWithStructure("cag", "(.)");
		RuleProbsForGrammarSemiAdaptive rpfgsa = new RuleProbsForGrammarSemiAdaptive(G, rna);
		// Derivation of cag (.):
		// S => B => ( B ) => ( . )
		// rule counts:
		//      S => A: 0,
		//      S => B: 1,
		//      S => C: 0,
		//      A => u: 0
		//      B => ( B ): 1,
		//      B => u: 1,
		//      C => u: 0,
		// rule probs:
		//      S => A: 0,
		//      S => B: 1,
		//      S => C: 0,
		//      A => u: NaN // undefined; 0/0
		//      B => ( B ): 0.5,
		//      B => u: 0.5,
		//      C => u: NaN,
		// But: converted to RNA grammar ...
		Assert.assertEquals(0, rpfgsa.ruleProbs().get(new Rule(S, A)), 0.0001);
		Assert.assertEquals(1, rpfgsa.ruleProbs().get(new Rule(S, B)), 0.0001);
		Assert.assertEquals(0, rpfgsa.ruleProbs().get(new Rule(S, C)), 0.0001);
		Assert.assertEquals(Double.NaN, rpfgsa.ruleProbs().get(new Rule(A, new PairOfCharTerminal(new PairOfChar('A', '.')))), 0.0001);
		Assert.assertEquals(Double.NaN, rpfgsa.ruleProbs().get(new Rule(C, new PairOfCharTerminal(new PairOfChar('U', '.')))), 0.0001);
		Assert.assertEquals(Double.NaN, rpfgsa.ruleProbs().get(new Rule(C, new PairOfCharTerminal(new PairOfChar('A', '.')))), 0.0001);
		Assert.assertEquals(Double.NaN, rpfgsa.ruleProbs().get(new Rule(C, new PairOfCharTerminal(new PairOfChar('C', '.')))), 0.0001);
		Assert.assertEquals(Double.NaN, rpfgsa.ruleProbs().get(new Rule(C, new PairOfCharTerminal(new PairOfChar('G', '.')))), 0.0001);
		Assert.assertEquals(0.5, rpfgsa.ruleProbs().get(new Rule(B, new PairOfCharTerminal(new PairOfChar('C', '(')), B, new PairOfCharTerminal(new PairOfChar('G', ')')))), 0.0001);
		Assert.assertEquals(0, rpfgsa.ruleProbs().get(new Rule(B, new PairOfCharTerminal(new PairOfChar('A', '(')), B, new PairOfCharTerminal(new PairOfChar('U', ')')))), 0.0001);
		Assert.assertEquals(0.5, rpfgsa.ruleProbs().get(new Rule(B, new PairOfCharTerminal(new PairOfChar('A', '.')))), 0.0001);
		Assert.assertEquals(0, rpfgsa.ruleProbs().get(new Rule(B, new PairOfCharTerminal(new PairOfChar('C', '.')))), 0.0001);
		Assert.assertEquals(0, rpfgsa.ruleProbs().get(new Rule(B, new PairOfCharTerminal(new PairOfChar('G', '.')))), 0.0001);
		Assert.assertEquals(0, rpfgsa.ruleProbs().get(new Rule(B, new PairOfCharTerminal(new PairOfChar('U', '.')))), 0.0001);
	}

}
