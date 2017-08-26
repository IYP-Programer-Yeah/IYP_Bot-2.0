package botelements;

import java.util.ArrayList;

/**
 * Created by HoseinGhahremanzadeh on 7/11/2017.
 */
public class Command {
    public String commandName;
    public ArrayList<String> tokens = new ArrayList<String>();
    public CommandFunction function;
    public int permission = BLACK;
    public boolean isCaseSensitive = false;
    public boolean noUpperBoundForTokenCount = false;

    public final static int ADMIN = 3;
    public final static int WHITE = 2;
    public final static int NONE = 1;
    public final static int BLACK = 0;


    @Override

    public boolean equals(Object obj) {
        Command input = (Command) obj;
        return (isCaseSensitive?commandName.equals(input.commandName):commandName.toLowerCase().equals(input.commandName.toLowerCase())) && (tokens.size() == input.tokens.size() || (input.noUpperBoundForTokenCount && tokens.size() > input.tokens.size())) && permission >= input.permission;
    }
    public static Command clone(Command command) {
        Command clone = new Command();
        clone.commandName = command.commandName;
        clone.tokens = command.tokens;
        clone.function = command.function;
        clone.permission = command.permission;
        clone.isCaseSensitive = command.isCaseSensitive;
        return clone;
    }
}
