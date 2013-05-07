package train;

import grammar.Event;
import grammar.Grammar;
import grammar.Rule;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import tree.Node;
import tree.Tree;
import treebank.Treebank;
import utils.CountMap;

/**
 * 
 * @author Reut Tsarfaty
 * 
 *         CLASS: Train
 * 
 *         Definition: a learning component Role: reads off a grammar from a
 *         treebank Responsibility: keeps track of rule counts
 * 
 */

public class Train {

	/**
	 * Implementation of a singleton pattern Avoids redundant instances in
	 * memory
	 */
	public static Train m_singTrainer = null;

	public static Train getInstance() {
		if (m_singTrainer == null) {
			m_singTrainer = new Train();
		}
		return m_singTrainer;
	}

	public static void main(String[] args) {

	}

	public Grammar train(Treebank myTreebank) {
		Grammar myGrammar = new Grammar();
		System.out.println(myTreebank.size());
		for (int i = 0; i < myTreebank.size(); i++) {
			Tree myTree = myTreebank.getAnalyses().get(i);
			if (myTree==null) continue;
			List<Rule> theRules = getRules(myTree);
			myGrammar.addAll(theRules);

		}
		calculateProbs(myGrammar.getSyntacticRules(),myGrammar.getRuleCounts());
		calculateProbs(myGrammar.getLexicalRules(), myGrammar.getRuleCounts());
		return myGrammar;
	}

	public void calculateProbs(Set<Rule> rules, CountMap<Rule> ruleCountMap){
		CountMap<String> rootCountMap=new CountMap<>();
		for (Rule rule:rules){
			if (rule.getLHS().getSymbols().size()!=1) {
				System.out.println("Rule is bad: "+rule);
				continue;
			}
			rootCountMap.add(rule.getLHS().getSymbols().get(0), ruleCountMap.get(rule));
		}
		for (Rule rule:rules){
			double minusLogProb=-1.0*Math.log10(
					(double)ruleCountMap.get(rule) /
					(double)rootCountMap.get(rule.getLHS().getSymbols().get(0)));
			rule.setMinusLogProb(minusLogProb);
		}
	}

	public List<Rule> getRules(Tree myTree) {
		List<Rule> theRules = new ArrayList<Rule>();

		List<Node> myNodes = myTree.getNodes();
		for (int j = 0; j < myNodes.size(); j++) {
			Node myNode = myNodes.get(j);
			if (myNode.isInternal()) {
				Event eLHS = new Event(myNode.getIdentifier());
				Iterator<Node> theDaughters = myNode.getDaughters().iterator();
				StringBuilder sb = new StringBuilder();
				while (theDaughters.hasNext()) {
					Node n = (Node) theDaughters.next();
					sb.append(n.getIdentifier());
					if (theDaughters.hasNext())
						sb.append(" ");
				}
				Event eRHS = new Event(sb.toString());
				Rule theRule = new Rule(eLHS, eRHS);
				if (myNode.isPreTerminal())
					theRule.setLexical(true);
				if (myNode.isRoot())
					theRule.setTop(true);
				theRules.add(theRule);
			}
		}
		return theRules;
	}

}
