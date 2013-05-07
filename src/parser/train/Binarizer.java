package train;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import bracketimport.TreeReader;

import tree.Node;
import tree.Tree;
import treebank.Treebank;

public class Binarizer {

	private int model;

	public Binarizer(int model) {
		this.model = model;
	}

	public void binarize(Treebank treebank) {
		for (Tree tree : treebank.getAnalyses()) {
			if (tree!=null)
				binarize(tree.getRoot());
		}
	}
	
	public void debinarize(Treebank treebank){
		debinarize(treebank.getAnalyses());
	}
	
	public void debinarize(Collection<Tree> trees){
		for (Tree tree : trees) {
			if (tree!=null)
				debinarize(tree.getRoot());
		}
	}

	private String joinner(List<String> str) {
		Iterator<String> it = str.iterator();
		if (!it.hasNext())
			return "";

		StringBuilder sb = new StringBuilder();
		for (;;) {
			String e = it.next();
			sb.append(e);
			if (!it.hasNext())
				return sb.toString();
			sb.append(',');
		}
	}
	
	private void debinarize(Node node){
		for (int i=node.getDaughters().size()-1;i>=0;i--){
			debinarize(node.getDaughters().get(i));
		}
		if (node.getIdentifier().contains("@")){
			for (Node daughter:node.getDaughters()){
				node.getParent().addDaughter(daughter);
				daughter.setParent(node);
			}
			node.getParent().removeDaughter(node);
		}
	}
	
	private void binarize(Node node) {
		if (node.getDaughters().size() > 2) {
			boolean hMarcov = model > -1;
			Node tempParent = node;
			
			//Cloning is required since we are changing the original list during the loop
			List<Node> daughters=new ArrayList<>(node.getDaughters());
			LinkedList<String> labels = new LinkedList<>();
			if (model != 0)
				labels.add(daughters.get(0).getIdentifier());
			
			for (Node next:daughters.subList(1, daughters.size()-1)){
				String labelStr = (hMarcov ? "/" : "") + joinner(labels)
						+ (hMarcov ? "/" : "");
				Node subNode = new Node(labelStr + "@" + node.getIdentifier());
				subNode.addDaughter(next);
				node.removeDaughter(next);
				tempParent.addDaughter(subNode);
				tempParent = subNode;
				labels.add(next.getIdentifier());
				if (hMarcov && labels.size() > model) {
					labels.removeFirst();
				}
			}
			Node lastDaughter=daughters.get(daughters.size()-1);
			tempParent.addDaughter(lastDaughter);
			node.removeDaughter(lastDaughter);
		}
		for (Node daughter : node.getDaughters()) {
			binarize(daughter);
		}
	}

	public static void main(String... args) {
		TreeReader tr = new TreeReader();
		String treeStr = "(TOP (S (yyQUOT yyQUOT) (S (VP (VB THIH)) (NP (NN NQMH)) (CC W) (ADVP (RB BGDWL))) (yyDOT yyDOT)))";
		Map<Integer, String> tests = new HashMap<Integer, String>();
		tests.put(
				-1,
				"(TOP (S (yyQUOT yyQUOT) (yyQUOT@S (S (VP (VB THIH)) (VP@S (NP (NN NQMH)) (VP,NP@S (CC W) (ADVP (RB BGDWL))))) (yyDOT yyDOT))))");
		tests.put(
				0,
				"(TOP (S (yyQUOT yyQUOT) (//@S (S (VP (VB THIH)) (//@S (NP (NN NQMH)) (//@S (CC W) (ADVP (RB BGDWL))))) (yyDOT yyDOT))))");
		tests.put(
				1,
				"(TOP (S (yyQUOT yyQUOT) (/yyQUOT/@S (S (VP (VB THIH)) (/VP/@S (NP (NN NQMH)) (/NP/@S (CC W) (ADVP (RB BGDWL))))) (yyDOT yyDOT))))");
		tests.put(
				2,
				"(TOP (S (yyQUOT yyQUOT) (/yyQUOT/@S (S (VP (VB THIH)) (/VP/@S (NP (NN NQMH)) (/VP,NP/@S (CC W) (ADVP (RB BGDWL))))) (yyDOT yyDOT))))");
		tests.put(
				3,
				"(TOP (S (yyQUOT yyQUOT) (/yyQUOT/@S (S (VP (VB THIH)) (/VP/@S (NP (NN NQMH)) (/VP,NP/@S (CC W) (ADVP (RB BGDWL))))) (yyDOT yyDOT))))");
		tests.put(
				4,
				"(TOP (S (yyQUOT yyQUOT) (/yyQUOT/@S (S (VP (VB THIH)) (/VP/@S (NP (NN NQMH)) (/VP,NP/@S (CC W) (ADVP (RB BGDWL))))) (yyDOT yyDOT))))");
		for (Entry<Integer, String> test : tests.entrySet()) {
			Tree tree = (Tree) tr.read(treeStr);
			Binarizer b = new Binarizer(test.getKey());
			b.binarize(tree.getRoot());
			System.out.println(test.getKey()+": "+test.getValue());
			boolean result=tree.toString().equals(test.getValue());
			if (!result){
				throw new AssertionError("hMarkov "+test.getKey()+": Tree should have been:\n"+test.getValue()+" but it is\n"+tree);
			}
			b.debinarize(tree.getRoot());
			result=tree.toString().equals(treeStr);
			if (!result){
				throw new AssertionError("hMarkov "+test.getKey()+": debinarization should have been:\n"+treeStr+" but it is\n"+tree);
			}
		}
		
		System.out.println();
		System.out.println(treeStr);
		Tree tree = (Tree) tr.read(treeStr);
		Binarizer b = new Binarizer(-1);
		b.binarize(tree.getRoot());
		System.out.println(tree);
		b.debinarize(tree.getRoot());
		System.out.println(tree);
		
	}
}
