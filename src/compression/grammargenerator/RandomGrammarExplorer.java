package compression.grammargenerator;

import compression.RuleProbType;
import compression.data.CachedDataset;
import compression.data.Dataset;
import compression.data.FolderBasedDataset;
import compression.grammar.NonTerminal;
import compression.grammar.RNAWithStructure;
import compression.grammar.Rule;
import compression.grammar.SecondaryStructureGrammar;
import compression.grammar.Terminal;
import compression.parser.SRFParser;
import compression.util.MyMultimap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Explores randomly generated {@link SecondaryStructureGrammar}s to find candidates
 * that are effective for compression. This class provides two primary strategies for
 * generating random grammars.
 * <p>
 * The generation process is handled by two overloaded {@code randomGrammar} methods:
 * <ul>
 *     <li>One method generates a grammar by selecting a fixed number of rules
 *     ({@code nRules}) from the set of all possible rules. This selection is done
 *     with replacement, so the final grammar may have fewer unique rules than the
 *     number selected.</li>
 *     <li>The other method includes each possible rule with a given probability
 *     ({@code ruleProb}), leading to a variable number of rules in the final grammar.</li>
 * </ul>
 * Both methods ensure a valid grammar is returned by retrying if a randomly
 * generated rule set is structurally inconsistent (e.g., contains non-terminals
 * with no production rules).
 * <p>
 *
 * TODO: describe {@code main} method
 *
 * @author Sebastian Wild (wild@liverpool.ac.uk)
 */
public class RandomGrammarExplorer extends AbstractGrammarExplorer {


	public static void main(String[] args) throws IOException {
		// Parameters: #nonterminals, #rules, keepBestK, small-dataset, full-dataset
		// Keep trying grammars and keep the best k grammars
		// according to the given dataset
		if (args.length < 4) {
			System.out.println("Usage: RandomGrammarExplorer #nonterminals #rules keepBestK full-dataset [seed] [small-dataset] [rule-prob-model]");
			System.out.println("\t #rules can be p between 0 and 1 in which case each rule will be included at random with prob p");
			System.out.println("\t rule-prob-model: one of 'static', 'semi-adaptive', 'adaptive'");
			System.exit(1);
		}
		int nNonterminals = Integer.parseInt(args[0]);
		int nRules;
		double ruleProb;
		try {
			nRules = Integer.parseInt(args[1]);
			ruleProb = -1;
		} catch (NumberFormatException e) {
			ruleProb = Double.parseDouble(args[1]);
			nRules = -1;
		}
		int nBestGrammarsToKeep = Integer.parseInt(args[2]);
		Dataset fullDataset = new CachedDataset(new FolderBasedDataset(args[3]));
		long seed;
		if (args.length > 4) {
			seed = Long.parseLong(args[4]);
		} else {
			seed = System.currentTimeMillis();
		}
		Dataset smallDataset = new CachedDataset(new FolderBasedDataset(args.length > 5 ? args[5] : "small-dataset"));
		Dataset parsableDataset = new CachedDataset(new FolderBasedDataset("minimal-parsable"));
		System.out.println("nNonterminals = " + nNonterminals);
		System.out.println("nRules = " + nRules);
		System.out.println("ruleProb = " + ruleProb);
		System.out.println("nBestGrammarsToKeep = " + nBestGrammarsToKeep);
		System.out.println("fullDataset = " + fullDataset);
		System.out.println("smallDataset = " + smallDataset);
		System.out.println("parsableDataset = " + parsableDataset);
		System.out.println("seed = " + seed);

		Random random = new Random(seed);

		RuleProbType model = args.length > 6 ? RuleProbType.fromString(args[6]) : RuleProbType.ADAPTIVE;
		System.out.println("rule prob model type = " + model);

		// TODO generalize?
		if (model == RuleProbType.STATIC) {
			System.out.println("Only adaptive / semi-adaptive rule probability model is supported at the moment. Sorry.");
			System.exit(1);
		}
		String fileName = "best-grammars-" + nNonterminals + "-NTs-" + (nRules < 0 ? ruleProb : nRules) + "-rules-seed-" + seed + "-" + model + ".txt";
		System.out.println("Writing best grammars to file " + new File(fileName).getAbsolutePath());

		SortedSet<GrammarWithScore> bestGrammars = new TreeSet<>();
		bestGrammars.add(new GrammarWithScore(null, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
		RandomGrammarExplorer explorer = new RandomGrammarExplorer(nNonterminals);

		// Cache minimal-parsable dataset
		List<List<Terminal<Character>>> parsableDatasetWords = new ArrayList<>(parsableDataset.getSize());
		for (RNAWithStructure rna : parsableDataset) {
			List<Terminal<Character>> terminals = rna.secondaryStructureAsTerminals();
			parsableDatasetWords.add(terminals);
		}

		long nGrammars = -1;
		next_grammar:
		while (true) {
			++nGrammars;
			// measure elapsed time
			long startTime = System.currentTimeMillis();
			SecondaryStructureGrammar grammar = nRules > 0 ?
					explorer.randomGrammar(random, nRules) :
					explorer.randomGrammar(random, ruleProb);
			System.out.println("\tgrammar " + (nGrammars) + " generated (" + (System.currentTimeMillis() - startTime) + " ms)");
			try {
				// Level 1 check: parses minimal-parsable?
				SRFParser<Character> ssParser = new SRFParser<>(grammar);
				for (List<Terminal<Character>> word : parsableDatasetWords) {
					if (!ssParser.parsable(word))
						continue next_grammar; // ignore this grammar
				}
				// Passed level 1
				System.out.println("Grammar passed level 1 (" + (System.currentTimeMillis() - startTime) + " ms)");
				System.out.println("grammar = " + grammar);


				// Level 2: determine bits per base compression ratio on small dataset
				double avgBitsPerBaseSmallDataset = getBitsPerBase(smallDataset, model, grammar, false);
				// if good enough, keep it and go to level 3
				if (bestGrammars.last().avgBitsPerBaseSmallDataset <= avgBitsPerBaseSmallDataset) {
					// ignore this grammar
					continue;
				}
				System.out.println("\tGrammar " + (nGrammars) + " passed level 2 (" + (System.currentTimeMillis() - startTime) + " ms)");

				// Level 3: determine bits per base compression ratio on full dataset
				double avgBitsPerBaseFullDataset = getBitsPerBase(fullDataset, model, grammar, true);
				GrammarWithScore e = new GrammarWithScore(grammar, avgBitsPerBaseFullDataset, avgBitsPerBaseSmallDataset);
				bestGrammars.add(e);
				System.out.println("\tGrammar " + (nGrammars) + " passed level 3  (" + (System.currentTimeMillis() - startTime) + " ms)");
				System.out.println("\tnew entry: " + e);
				if (bestGrammars.size() > nBestGrammarsToKeep) {
					bestGrammars.remove(bestGrammars.last());
				}
				try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName)))) {
					printGrammars(out, bestGrammars);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				System.out.println("Best grammars so far:");
				printGrammars(new PrintWriter(System.out), bestGrammars);
			} catch (Exception e) {
				System.err.println("Didn't except this: " + e);
				System.err.println("Grammar " + grammar.name + " is invalid.");
				System.err.println(grammar);
				e.printStackTrace();
				System.out.println("Continue with next grammar anyways.");
			}
		}

	}

	private int[] usedRules;


	public RandomGrammarExplorer(final int nNonterminals) {
		super(nNonterminals);
		usedRules = new int[allPossibleRules.length];
	}

	/**
	 * Generates a {@link SecondaryStructureGrammar} where each possible rule is included with a given probability.
	 * <p>
	 * This method iterates through all possible rules ({@link #allPossibleRules}) and includes each
	 * rule in the generated grammar with a probability of {@code ruleProb}. This probabilistic approach
	 * means the exact number of rules in the resulting grammar is not fixed. The method ensures
	 * that the generated grammar is valid (i.e., does not throw an {@link IllegalArgumentException}
	 * upon instantiation). If an invalid grammar is created (e.g., one with no rules for a non-terminal),
	 * the process is repeated until a valid one is successfully generated.
	 * <p>
	 * The name of the generated grammar is constructed to include the number of non-terminals and a
	 * bitmask representing the set of rules chosen.
	 *
	 * @param random   the {@link Random} object to use for the probabilistic selection.
	 * @param ruleProb the probability (between 0.0 and 1.0) of including each possible rule.
	 * @return A randomly generated, valid {@link SecondaryStructureGrammar}.
	 * @throws IllegalArgumentException if {@code ruleProb} is not in the range [0, 1].
	 */
	private SecondaryStructureGrammar randomGrammar(final Random random, final double ruleProb) {
		while (true) {
			try {
				if (ruleProb < 0 || ruleProb > 1) {
					throw new IllegalArgumentException("ruleProb must be between 0 and 1");
				}
				MyMultimap<NonTerminal, Rule> rules = new MyMultimap<>();
				Arrays.fill(usedRules, 0);
				for (int i = 0; i < allPossibleRules.length; i++) {
					if (random.nextDouble() > ruleProb) continue;
					Rule rule = allPossibleRules[i];
					usedRules[i] = 1;
					rules.put(rule.left, rule);
				}
				String name = "RandomGrammar_" + nNonterminals + "_" +
						(Arrays.stream(usedRules).mapToObj(Integer::toString).collect(Collectors.joining("")));
				SecondaryStructureGrammar G = new SecondaryStructureGrammar(name, nonTerminals[nNonterminals - 1], rules);
				//				System.out.println("G = " + G);
				return G;
			} catch (IllegalArgumentException e) {
				// ignore, created nonsense grammar
			}
		}
	}

	/**
	 * Generates a {@link SecondaryStructureGrammar} with a specified number of randomly chosen rules.
	 * <p>
	 * This method attempts to create a grammar by selecting at most {@code nRules} rules at random from the
	 * set of all possible rules (see the inherited field {@link #allPossibleRules}). The selection is done with replacement,
	 * so the same rule can be picked multiple times, though it will only be added to the grammar's
	 * rule set once. The method ensures that the generated grammar is valid (i.e., does not throw
	 * an {@link IllegalArgumentException} upon instantiation). If an invalid grammar is created,
	 * the process is repeated until a valid one is successfully generated.
	 * <p>
	 * The name of the generated grammar is constructed to include the number of non-terminals,
	 * the number of rules, and the set of rules chosen.
	 *
	 * @param random  the {@link Random} object to use for selecting rules.
	 * @param nRules  the target number of rules to *attempt* to select for the grammar.
	 *                Because rules are selected with replacement, the final grammar may contain
	 *                fewer than {@code nRules} distinct rules. This value must be between 1 and the
	 *                total number of possible rules.
	 * @return A randomly generated, valid {@link SecondaryStructureGrammar}.
	 * @throws IllegalArgumentException if {@code nRules} is not within the valid range.
	 */
	public SecondaryStructureGrammar randomGrammar(final Random random, final int nRules) {
		while (true) { // retry until grammar is valid
			try {
				if (nRules < 1 || nRules > allPossibleRules.length) {
					throw new IllegalArgumentException("nRules must be between 1 and " + allPossibleRules.length);
				}
				MyMultimap<NonTerminal, Rule> rules = new MyMultimap<>();
				Arrays.fill(usedRules, 0);
				for (int i = 0; i < nRules; i++) {
					int ruleIndex = random.nextInt(allPossibleRules.length);
					usedRules[ruleIndex] = 1;
					Rule rule = allPossibleRules[ruleIndex];
					rules.put(rule.left, rule);
				}
				String name = "RandomGrammar_" + nNonterminals + "_" + nRules + "_" +
						(Arrays.stream(usedRules).mapToObj(Integer::toString).collect(Collectors.joining("")));
				SecondaryStructureGrammar G = new SecondaryStructureGrammar(name, nonTerminals[nNonterminals - 1], rules);
//				System.out.println("G = " + G);
				return G;
			} catch (IllegalArgumentException e) {
				// ignore, created nonsense grammar
			}
		}
	}

}
