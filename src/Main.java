import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("input path is required");
        }
        String source = args[0];
        CharStream input = CharStreams.fromFileName(source);
        SysYLexerLexer lexer = new SysYLexerLexer(input);
        lexer.removeErrorListeners();
        myErrorListener errorListener = new myErrorListener();
        lexer.addErrorListener(errorListener);
        String[] RuleNames = lexer.getRuleNames();
        for (Token token : lexer.getAllTokens()) {
            if (errorListener.hasError) {
                break;
            }
            String text = token.getText();
            if (token.getType() == SysYLexerLexer.INTEGER_CONST) {
                if (text.startsWith("0X") || text.startsWith("0x")) {
                    text = String.valueOf(Integer.parseInt(text.substring(2), 16));
                } else if (text.startsWith("0")) {
                    text = String.valueOf(Integer.parseInt(text, 8));
                }
            }
            System.err.println(RuleNames[token.getType() - 1] + " " + text + " at Line " + token.getLine() + ".");
        }
    }
}