public class FunctionSymbol extends BaseScope implements Symbol {
	public FunctionType type;
	@Override
	public FunctionType getType() {
		return type;
	}
	
	public FunctionSymbol(String name, Scope enclosingScope, FunctionType type) {
		super(name, enclosingScope);
		this.type = type;
	}
}