import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("input path is required");
        }
        String source = args[0];
        CharStream input = CharStreams.fromFileName(source);
        SysYLexer lexer = new SysYLexer(input);
        lexer.removeErrorListeners();
        lexerErrorListener errorListener = new lexerErrorListener();
        lexer.addErrorListener(errorListener);
//        String[] RuleNames = lexer.getRuleNames();
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SysYParser parser = new SysYParser(tokens);
        parser.removeErrorListeners();
        parserErrorListener parserErrorListener = new parserErrorListener();
        parser.addErrorListener(parserErrorListener);


        ParseTree tree = parser.program();
        //Visitor extends SysYParserBaseVisitor<Void>
        if (parserErrorListener.hasErrors()) {
            return;
        }
        parserVisitor visitor = new parserVisitor();
        visitor.visit(tree);

        if (!visitor.isError()) {
            for (Object s : visitor.getPrintMsg()) {
                System.err.println(s);
            }
        }

    }
}