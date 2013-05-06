package decode;

import grammar.Event;
import grammar.Grammar;
import grammar.Rule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tree.Node;
import tree.Terminal;
import tree.Tree;

public class Decode {

	public static Set<Rule> m_setGrammarRules = null;
	public static Map<String, Set<Rule>> m_mapLexicalRules = null;

	/**
	 * Implementation of a singleton pattern Avoids redundant instances in
	 * memory
	 */
	public static Decode m_singDecoder = null;

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

//		@Override
//		public int hashCode() {
//			final int prime = 31;
//			int result = 1;
//			result = prime * result + getOuterType().hashCode();
//			result = prime * result + ((child1 == null) ? 0 : child1.hashCode());
//			result = prime * result + ((child2 == null) ? 0 : child2.hashCode());
//			result = prime * result + ((tag == null) ? 0 : tag.hashCode());
//			return result;
//		}
//
//		@Override
//		public boolean equals(Object obj) {
//			if (this == obj)
//				return true;
//			if (obj == null)
//				return false;
//			if (getClass() != obj.getClass())
//				return false;
//			TagProb other = (TagProb) obj;
//			if (!getOuterType().equals(other.getOuterType()))
//				return false;
//			if (child1 == null) {
//				if (other.child1 != null)
//					return false;
//			} else if (!child1.equals(other.child1))
//				return false;
//			if (child2 == null) {
//				if (other.child2 != null)
//					return false;
//			} else if (!child2.equals(other.child2))
//				return false;
//			if (tag == null) {
//				if (other.tag != null)
//					return false;
//			} else if (!tag.equals(other.tag))
//				return false;
//			return true;
//		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((tag == null) ? 0 : tag.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TagProb other = (TagProb) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (tag == null) {
				if (other.tag != null)
					return false;
			} else if (!tag.equals(other.tag))
				return false;
			return true;
		}

		private Decode getOuterType() {
			return Decode.this;
		}

	}

	private TagProb getTagProb(Set<TagProb> cell, String symbol) {
		if (cell == null)
			return null;
		// TODO split to two charts, for quicker search
		for (TagProb tp : cell) {
			List<String> symbols = tp.getTag().getSymbols();
			if (symbols.size() == 1 && symbols.get(0).equals(symbol))
				return tp;
		}
		return null;
	}

	private void addTagProb(Set<TagProb> cell, TagProb tp) {
		TagProb exists = getTagProb(cell, tp.getTag().toString());
		if (exists != null) {
			if (tp.getMinusLogProb() > exists.getMinusLogProb())
				return;
			cell.remove(exists);
		}
		cell.add(tp);
	}

	public Tree decode(List<String> input) {
		Tree tree=ckyParse(input);
		if (tree==null){
			System.out.println("Input "+input+" has failed decoding");
			// if CKY failed, use baseline parser
			tree=dummyParse(input);
		}

		return tree;

	}

	private Tree ckyParse(List<String> input) {
		Set<TagProb>[][] chart = new Set[input.size()][];

		// init
		for (int i = 0; i < input.size(); i++) {
			String seg = input.get(i);
			chart[i] = (Set<TagProb>[])new Set[input.size() + 1];
			Set<Rule> rules = m_mapLexicalRules.get(seg);
			if (rules == null || rules.isEmpty()) {
				rules = new HashSet<>();
				Rule nnRule = new Rule("NN", seg, true);
				nnRule.setMinusLogProb(0.0);
				// System.out.println("ADDED SMOOTHED RULE "+nnRule);
				rules.add(nnRule);
			} else {
				rules = new HashSet<>(rules);
			}
			System.out.println("For " + seg + " the rules are " + rules);
			for (Rule rule : rules) {
				if (chart[i][i + 1] == null)
					chart[i][i + 1] = new HashSet<>();
				if (chart[i][i] == null)
					chart[i][i] = new HashSet<>();
				TagProb lexTagProb = new TagProb(rule.getRHS());
				chart[i][i].add(lexTagProb);
				TagProb synTagProb = new TagProb(rule.getLHS(), rule.getMinusLogProb(), lexTagProb);
				chart[i][i + 1].add(synTagProb);
			}
			// TODO unary extension

			// Set<Rule> toAdd;
			// do {
			// toAdd = new HashSet<>();
			// for (Rule rule : m_setGrammarRules) {
			// List<String> symbols = rule.getRHS().getSymbols();
			// // System.out.println("!!"+rules);
			// for (Rule lexRule : rules) {
			// if (symbols.size() == 1
			// && symbols.get(0).equals(
			// lexRule.getLHS().getSymbols().get(0))
			// && !rules.contains(rule)) {
			// System.out.println("need to add " + rule
			// + " to reach " + lexRule);
			// toAdd.add(rule);
			// }
			// }
			//
			// }
			// if (!toAdd.isEmpty()) {
			// rules.addAll(toAdd);
			// System.out.println("  adding " + toAdd);
			// }
			// } while (!toAdd.isEmpty());
			// System.out.println("For " + seg + " the rules are " + rules);
			// // for (Rule rule:rules){
			// // m_mapLexicalRules.
			// // }
			// chart[i][i + 1] = rules;

		}
		// printChart(chart);

		for (int i = 2; i <= input.size(); i++) { // row
			for (int j = i - 2; j >= 0; j--) { // col
				for (int k = j + 1; k <= i - 1; k++) {
					System.out.println("Loop! j,i (" + j + "," + i + ") k=" + k);
					if (chart[j][i] == null)
						chart[j][i] = new HashSet<>();
					for (Rule r : m_setGrammarRules) {
						Event lhs = r.getLHS();
						Event rhs = r.getRHS();
						List<String> symbols = rhs.getSymbols();
						if (symbols.size() == 2) {
							// System.out.println("Getting chart[" + j + "][" +
							// k + "]");

							TagProb left = getTagProb(chart[j][k], symbols.get(0));
							// System.out.println("Getting chart[" + k + "][" +
							// i + "]");
							TagProb right = getTagProb(chart[k][i], symbols.get(1));
							if (left != null && right != null) {
								System.out.println("  left sym  " + symbols.get(0));
								System.out.println("  right sym " + symbols.get(1));
								System.out.println("  adding probs for " + r + "[" + r.getMinusLogProb() + "]");
								System.out.println("  left  " + left);
								System.out.println("  right " + right);
								double minusLogProb = left.getMinusLogProb() + right.getMinusLogProb()
										+ r.getMinusLogProb();
								TagProb tp = new TagProb(lhs, minusLogProb, left, right);
								System.out.println("  adding " + tp);
								addTagProb(chart[j][i], tp);
							}
						}
					}

				}
			}
		}
		printChart(chart);
		System.out.println();
		System.out.println(chart[0][input.size()]);
		TagProb start = null;
		for (TagProb tp : chart[0][input.size()]) {
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
		System.out.println(tree);
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

	private void printChart(Set<TagProb>[][] chart) {
		int[] width = new int[chart[0].length];
		for (int i = 0; i < chart.length; i++) {
			for (int j = 0; j < chart[i].length; j++) {
				int thisWidth = chart[i][j] != null ? chart[i][j].toString().length() : 0;
				if (thisWidth > width[j])
					width[j] = thisWidth;
			}
		}
		for (int i = 0; i < chart.length; i++) {
			for (int j = 0; j < chart[i].length; j++) {
				String format = "%-" + width[j] + "s";
				if (chart[i][j] == null)
					System.out.print(String.format(format, ""));
				else
					System.out.print(String.format(format, chart[i][j]));
				System.out.print("|");
			}
			System.out.println();
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
