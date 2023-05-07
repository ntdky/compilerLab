public class GlobalScope extends BaseScope {
	public GlobalScope(Scope enclosingScope) {
		super(Constant.GlobalScope, enclosingScope);
		putSymbol(new BasicTypeSymbol(Constant.INT));
		putSymbol(new BasicTypeSymbol(Constant.VOID));
	}
}