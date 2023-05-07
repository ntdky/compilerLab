import java.util.LinkedHashMap;
import java.util.Map;

public class BaseScope implements Scope {
	private final Scope enclosingScope;
	private final Map<String, Symbol> symbols = new LinkedHashMap<>();
	private String name;
	
	public BaseScope(String name, Scope enclosingScope) {
		this.name = name;
		this.enclosingScope = enclosingScope;
	}
	
	@Override
	public boolean isHaveSymbol(String name) {
		return symbols.containsKey(name);
	}
	
	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public Scope getEnclosingScope() {
		return this.enclosingScope;
	}
	
	@Override
	public void putSymbol(Symbol symbol) {
		symbols.put(symbol.getName(), symbol);
	}
	
	@Override
	public Symbol getSymbol(String name) {
		Symbol symbol = symbols.get(name);
		if (symbol != null) {
			return symbol;
		}
		if (enclosingScope != null) {
			return enclosingScope.getSymbol(name);
		}
		
		return null;
	}
}
