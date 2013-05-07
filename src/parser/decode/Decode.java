package decode;

import grammar.Event;
import grammar.Grammar;
import grammar.Rule;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tree.Node;
import tree.Terminal;
import tree.Tree;
import utils.Timer;

public class Decode {

	private static final boolean OUTPUT = false;

	public static Set<Rule> m_setGrammarRules = null;
	public static Map<String, Set<Rule>> m_mapLexicalRules = null;

	/**
	 * Implementation of a singleton pattern Avoids redundant instances in
	 * memory
	 */
	public static Decode m_singDecoder = null;

	public static void print(Object o) {
		if (OUTPUT)
			System.out.print(o);
	}

	public static void println(Object o) {
		if (OUTPUT)
			System.out.println(o);
	}

	public static Decode getInstance(Grammar g) {
		if (m_singDecoder == null) {
			m_singDecoder = new Decode();
			m_setGrammarRules = g.getSyntacticRules();
			m_mapLexicalRules = g.getLexicalEntries();
		}
		return m_singDecoder;
	}

	private class TagProb {
		final private double minusLogProb;
		final private Event tag;
		final private TagProb child1;
		final private TagProb child2;

		final private boolean isLex;

		private TagProb(Event tag, double minusLogProb, TagProb child1, TagProb child2, boolean isLex) {
			this.tag = tag;
			this.minusLogProb = minusLogProb;
			this.child1 = child1;
			this.child2 = child2;
			this.isLex = isLex;
		}

		public TagProb(Event tag, double minusLogProb, TagProb child1, TagProb child2) {
			this(tag, minusLogProb, child1, child2, false);
			assert child1 != null;
			assert child2 != null;
		}

		public TagProb(Event tag, double minusLogProb, TagProb child) {
			this(tag, minusLogProb, child, null, false);
			assert child != null;
		}

		public TagProb(Event tag) {
			this(tag, -0.0, null, null, true);
		}

		public double getMinusLogProb() {
			return minusLogProb;
		}

		public Event getTag() {
			return tag;
		}

		public TagProb getChild1() {
			return child1;
		}

		public TagProb getChild2() {
			return child2;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (child1 == null)
				sb.append('"');
			sb.append(tag.toString());
			if (child1 != null) {
				sb.append("->(");
				if (child1.isLex)
					sb.append('"');
				sb.append(child1.tag);
				if (child1.isLex)
					sb.append('"');
				if (child2 != null)
					sb.append(' ').append(child2.tag);
				sb.append(")[").append(String.format("%.2f", minusLogProb)).append(']');
			} else
				sb.append('"');
			return sb.toString();
		}

	}

	private TagProb getTagProb(TagProbCell cell, String symbol) {
		if (cell == null)
			return null;
		return cell.get(symbol);
		// if (cell.containsKey(symb))
		// for (TagProb tp : cell) {
		// List<String> symbols = tp.getTag().getSymbols();
		// if (symbols.size() == 1 && symbols.get(0).equals(symbol))
		// return tp;
		// }
		// return null;
	}

	private void addTagProb(TagProbCell cell, TagProb tp) {
		cell.addTagProb(tp);
		// TagProb exists = getTagProb(cell, tp.getTag().toString());
		// if (exists != null) {
		// if (tp.getMinusLogProb() > exists.getMinusLogProb())
		// return;
		// cell.remove(exists);
		// }
		// cell.add(tp);
	}

	public Tree decode(List<String> input) {
		Tree tree = null;
		if (input.size() <= 40)
			tree = ckyParse(input);

		if (tree == null) {
			if (input.size() > 40) {
				System.out.println("Skipping CKY parsing (too large) of input " + input);
			} else {
				System.out.println("Skipping CKY parsing (failed) of input " + input);
			}
			// if CKY failed, use baseline parser
			tree = dummyParse(input);
		}

		return tree;

	}

	private class TagProbCell extends HashMap<String, TagProb> {
		private static final long serialVersionUID = 7657267520681027890L;

		public void addTagProb(TagProb tp) {
			String sym = tp.getTag().toString();
			if (this.containsKey(sym)) {
				TagProb existing = get(sym);
				if (existing.getMinusLogProb() < tp.getMinusLogProb()) {
					return;
				}
			}
			this.put(sym, tp);
		}
	}

	private Tree ckyParse(List<String> input) {
		TagProbCell[][] chart = new TagProbCell[input.size()][];

		Set<Rule> biRules = new HashSet<>();
		Set<Rule> uniRules = new HashSet<>();
		for (Rule r : m_setGrammarRules) {
			if (r.getRHS().getSymbols().size() == 2) {
				biRules.add(r);
			} else {
				uniRules.add(r);
				println("Unary rule " + r);
			}
		}
		// init
		for (int i = 0; i < input.size(); i++) {
			String seg = input.get(i);
			chart[i] = new TagProbCell[input.size() + 1];
			Set<Rule> lexRules = m_mapLexicalRules.get(seg);
			if (lexRules == null || lexRules.isEmpty()) {
				lexRules = new HashSet<>();
				Rule nnRule = new Rule("NN", seg, true);
				nnRule.setMinusLogProb(0.0);
				// println("ADDED SMOOTHED RULE "+nnRule);
				lexRules.add(nnRule);
			} else {
				lexRules = new HashSet<>(lexRules);
			}
			println("For " + seg + " the rules are " + lexRules);

			// Add lex rules
			for (Rule rule : lexRules) {
				if (chart[i][i + 1] == null)
					chart[i][i + 1] = new TagProbCell();
				if (chart[i][i] == null)
					chart[i][i] = new TagProbCell();
				TagProb lexTagProb = new TagProb(rule.getRHS());
				chart[i][i].addTagProb(lexTagProb);
				// chart[i][i].add(lexTagProb);
				TagProb synTagProb = new TagProb(rule.getLHS(), rule.getMinusLogProb(), lexTagProb);
				// chart[i][i + 1].add(synTagProb);
				chart[i][i + 1].addTagProb(synTagProb);
			}

			// Add unary rules
			// TODO add several iterations
			for (Rule rule : uniRules) {
				Event lhs = rule.getLHS();
				Event rhs = rule.getRHS();
				List<String> symbols = rhs.getSymbols();
				TagProb child = getTagProb(chart[i][i + 1], symbols.get(0));
				if (child != null) {
					double minusLogProb = child.getMinusLogProb() + rule.getMinusLogProb();
					TagProb tp = new TagProb(lhs, minusLogProb, child);
					println("  adding unary rule " + tp);
					addTagProb(chart[i][i + 1], tp);
				}
			}
			// TODO unary extension

			// Set<Rule> toAdd;
			// do {
			// toAdd = new HashSet<>();
			// for (Rule rule : m_setGrammarRules) {
			// List<String> symbols = rule.getRHS().getSymbols();
			// // println("!!"+rules);
			// for (Rule lexRule : rules) {
			// if (symbols.size() == 1
			// && symbols.get(0).equals(
			// lexRule.getLHS().getSymbols().get(0))
			// && !rules.contains(rule)) {
			// println("need to add " + rule
			// + " to reach " + lexRule);
			// toAdd.add(rule);
			// }
			// }
			//
			// }
			// if (!toAdd.isEmpty()) {
			// rules.addAll(toAdd);
			// println("  adding " + toAdd);
			// }
			// } while (!toAdd.isEmpty());
			// println("For " + seg + " the rules are " + rules);
			// // for (Rule rule:rules){
			// // m_mapLexicalRules.
			// // }
			// chart[i][i + 1] = rules;

		}
		// printChart(chart);
		println("Done initiating, size " + input.size());
		Timer t;
		for (int i = 2; i <= input.size(); i++) { // row

			for (int j = i - 2; j >= 0; j--) { // col
				Timer innr = new Timer().start();
				for (int k = j + 1; k <= i - 1; k++) {
					if (chart[j][i] == null) {
						chart[j][i] = new TagProbCell();
					}

					for (Rule r : biRules) {
						Event lhs = r.getLHS();
						Event rhs = r.getRHS();
						List<String> symbols = rhs.getSymbols();

						TagProb left = getTagProb(chart[j][k], symbols.get(0));
						TagProb right = getTagProb(chart[k][i], symbols.get(1));

						if (left != null && right != null) {
							// println("--left sym  " + symbols.get(0));
							// println("  right sym " + symbols.get(1));
							// println("  adding probs for " + r + "[" +
							// r.getMinusLogProb() + "]");
							// println("  left  " + left);
							// println("  right " + right);
							double minusLogProb = left.getMinusLogProb() + right.getMinusLogProb()
									+ r.getMinusLogProb();
							TagProb tp = new TagProb(lhs, minusLogProb, left, right);
							// println("  adding " + tp);
							addTagProb(chart[j][i], tp);
						}
					}

					for (Rule r : uniRules) {
						Event lhs = r.getLHS();
						Event rhs = r.getRHS();
						List<String> symbols = rhs.getSymbols();
						TagProb child = getTagProb(chart[j][i], symbols.get(0));
						if (child != null) {
							// println("--child sym " + symbols.get(0));
							// println("  adding probs for " + r + "[" +
							// r.getMinusLogProb() + "]");
							// println("  child " + child);
							double minusLogProb = child.getMinusLogProb() + r.getMinusLogProb();
							TagProb tp = new TagProb(lhs, minusLogProb, child);
							// println("  adding " + tp);
							addTagProb(chart[j][i], tp);
						}
					}
				}
				// System.out.println("For "+j+","+i+" "+((i - 1)-(j +
				// 1))+" it took "+innr.stop());
			}
		}
		printChart(chart);
		println("");
		println(chart[0][input.size()]);
		TagProb start = null;
		for (TagProb tp : chart[0][input.size()].values()) {
			if (tp.tag.toString().equals("S")) {
				if (start == null || start.getMinusLogProb() > tp.getMinusLogProb())
					start = tp;
			}
		}
		if (start == null)
			return null;
		Node top = new Node("TOP");
		Node s = new Node("S");
		s.setParent(top);
		top.addDaughter(s);
		buildTree(s, start);
		Tree tree = new Tree(top);
		println(tree);
		return tree;
	}

	private void buildTree(Node node, TagProb tp) {
		node.setIdentifier(tp.getTag().toString());
		if (tp.getChild1() != null) {
			Node d1 = new Node(tp.getChild1().getTag().toString());
			node.addDaughter(d1);
			d1.setParent(node);
			buildTree(d1, tp.getChild1());
		}
		if (tp.getChild2() != null) {
			Node d2 = new Node(tp.getChild1().getTag().toString());
			node.addDaughter(d2);
			d2.setParent(node);
			buildTree(d2, tp.getChild2());
		}

	}

	private void printChart(TagProbCell[][] chart) {
		int[] width = new int[chart[0].length];
		for (int i = 0; i < chart.length; i++) {
			for (int j = 0; j < chart[i].length; j++) {
				int thisWidth = chart[i][j] != null ? chart[i][j].values().toString().length() : 0;
				if (thisWidth > width[j])
					width[j] = thisWidth;
			}
		}
		for (int i = 0; i < chart.length; i++) {
			for (int j = 0; j < chart[i].length; j++) {
				String format = "%-" + width[j] + "s";
				if (chart[i][j] == null)
					print(String.format(format, ""));
				else
					print(String.format(format, chart[i][j].values()));
				print("|");
			}
			println("");
		}
	}

	private Tree dummyParse(List<String> input) {
		Tree t = new Tree(new Node("TOP"));
		Iterator<String> theInput = input.iterator();
		while (theInput.hasNext()) {
			String theWord = theInput.next();
			Node preTerminal = new Node("NN");
			Terminal terminal = new Terminal(theWord);
			preTerminal.addDaughter(terminal);
			t.getRoot().addDaughter(preTerminal);
		}
		return t;
	}

	public static void main(String... args) {
		System.out.println("A" + String.format("%-5s", "b") + "B");
	}

}
