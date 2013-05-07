package grammar;


import java.util.HashSet;
import java.util.Set;

/**
 * 
 * @author rtsarfat
 *
 * CLASS: Grammar
 * 
 * Definition: A generative grammar rule of the form RHS --> LHS
 * Role: Know the Context for the application of an re-write event
 * Responsibility: 
 * -- Distinguish terminals and non-terminals, 
 * -- Distinguish lexical rules and syntactic rules
 * -- Assign probabilities
 * 
 */

public class Rule {

	protected Event m_LHS = null;
	protected Event m_RHS = null;
	protected double m_dMinusLogProb = Double.POSITIVE_INFINITY;

	protected boolean m_bLexical = false;
	protected boolean m_bTop = false;
	
	public double getMinusLogProb() {
		return m_dMinusLogProb;
	}

	public void setMinusLogProb(double m_dMinusLogProb) {
		this.m_dMinusLogProb = m_dMinusLogProb;
	}

	public Rule(String s1, String s2){
		setLHS(new Event(s1));
		setRHS(new Event(s2));
	}
	
	public Rule(String s1, String s2, boolean lex){
		setLHS(new Event(s1));
		setRHS(new Event(s2));
		setLexical(lex);
	}
	
	public Rule(String s1, String s2, boolean lex, boolean top){
		setLHS(new Event(s1));
		setRHS(new Event(s2));
		setLexical(lex);
		setTop(top);
	}
	
	public Rule(Event e1, Event e2) {
		setLHS(e1);
		setRHS(e2);
	}

	public Rule(Event e1, Event e2, boolean bLex) {
		setLHS(e1);
		setRHS(e2);
		setLexical(bLex);
	}
	
	public boolean isTop() {
		return m_bTop;
	}

	public void setTop(boolean m_bTop) {
		this.m_bTop = m_bTop;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		Event e1 = new Event ("A");
		Event e2 = new Event ("A");
		Event e3 = new Event ("B C");
		Event e4 = new Event ("B C");
		
		Rule r1 = new Rule(e1,e3);
		Rule r2 = new Rule(e2,e4);

		System.out.println("Rule 1: " + r1);
		System.out.println("Rule 2: " + r2);
		
		Set<Rule> set = new HashSet<Rule>();
		set.add(r1);
		set.add(r2);
		System.out.println("TEST/ hash-codes equal? " +(r1.hashCode() == r2.hashCode()));
		System.out.println("TEST/ rules equal? " +r1.equals(r2));
		System.out.println("TEST/ rule contained? " + (set.contains(r1) == true));
		System.out.println("TEST/ rule contained? " + (set.contains(r2) == true));
		set.remove(r2);
		System.out.println("TEST/ rule contained? " + (set.contains(r2) == false));
		
	}
	
//	public boolean equals(Object o)
//	{
//		return getLHS().equals(((Rule)o).getLHS()) 
//		 && getRHS().equals(((Rule)o).getRHS());
//	}
//	
//	public int hashCode()
//	{
//		return toString().hashCode();
//	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((m_LHS == null) ? 0 : m_LHS.hashCode());
		result = prime * result + ((m_RHS == null) ? 0 : m_RHS.hashCode());
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
		Rule other = (Rule) obj;
		if (m_LHS == null) {
			if (other.m_LHS != null)
				return false;
		} else if (!m_LHS.equals(other.m_LHS))
			return false;
		if (m_RHS == null) {
			if (other.m_RHS != null)
				return false;
		} else if (!m_RHS.equals(other.m_RHS))
			return false;
		return true;
	}

	public Event getLHS() {
		return m_LHS;
	}

	private void setLHS(Event m_lhs) {
		m_LHS = m_lhs;
	}

	public Event getRHS() {
		return m_RHS;
	}

	private void setRHS(Event m_rhs) {
		m_RHS = m_rhs;
	}
	
	private String _toString;
	public String toString()
	{
		if (_toString==null){
			_toString=getLHS().toString()+"-->"+getRHS().toString(); 
		}
		return _toString;
	}


	public boolean isLexical() {
		return m_bLexical;
	}

	public void setLexical(boolean lexical) {
		m_bLexical = lexical;
	}
	

}
