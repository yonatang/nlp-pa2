package grammar;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;


/**
 * 
 * @author rtsarfat
 *
 * CLASS: Event
 * 
 * Definition: 
 * 		Structured events 
 * Role: 
 * 		Define the form of the 
 *      left-hand-side and right-hand-side of grammar rules 
 * Responsibility: 
 * 		keep track of symbols lists, 
 * 		check event identity
 * 
 * Usage: Each event may define multiple daughters separated by space
 *   
 */

public class Event {
	
	private List<String> m_lstSymbols = new ArrayList<String>();
	
	public Event(String s) {
		StringTokenizer st = new StringTokenizer(s);
		while (st.hasMoreTokens()) {
			String sym = (String) st.nextToken();
			addSymbol(sym);
		}
	}

	private void addSymbol(String sym) {
		getSymbols().add(sym);
	}

	
	
	public String toString()
	{
		// return concatenation of symbols
		StringBuffer sb = new StringBuffer();
		Iterator<String> it = getSymbols().iterator();
		while (it.hasNext()) {
			String s = (String) it.next();
			sb.append(s);
			if (it.hasNext()) 
				sb.append(" ");	
		}
		return sb.toString();
	}
	
//	public boolean equals(Object o)
//	{
//		return toString().equals(((Event)o).toString());
//	}
//	public int hashCode()
//	{
//		return toString().hashCode();
//	}

	public List<String> getSymbols() 
	{
		return m_lstSymbols;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((m_lstSymbols == null) ? 0 : m_lstSymbols.hashCode());
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
		Event other = (Event) obj;
		if (m_lstSymbols == null) {
			if (other.m_lstSymbols != null)
				return false;
		} else if (!m_lstSymbols.equals(other.m_lstSymbols))
			return false;
		return true;
	}

	public void setSymbols(List<String> symbols) 
	{
		m_lstSymbols = symbols;
	}

	
	
	
	
}

