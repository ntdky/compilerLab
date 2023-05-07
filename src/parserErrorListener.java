import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.List;

public class parserErrorListener extends BaseErrorListener {

    private final List<String> errors = new ArrayList<>();
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        String error = "Error type B at Line " + line + ": " + msg;
        errors.add(error);
        System.err.println(error);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
