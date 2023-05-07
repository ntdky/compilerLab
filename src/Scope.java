public interface Scope {
	String getName();
	
	void setName(String name);
	
	Scope getEnclosingScope();
	
	void putSymbol(Symbol symbol);
	
	Symbol getSymbol(String name);
	
	boolean isHaveSymbol(String name);
}