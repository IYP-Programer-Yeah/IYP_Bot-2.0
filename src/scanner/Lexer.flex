package scanner;

import botelements.*;

%%
%class Scanner

%unicode
%line
%column
%public
%type Command
%{
	private Command command;
	private String currentToken;
	private int bracketCounter;
	
    public int getLine() {
        return yyline + 1;
    }
	
	public int getColumn() {
		return yycolumn + 1;
	}
%}

EOL = \r|\n|\r\n
Command = [\.\?\+\-\*\/\?\!\@\'a-zA-Z0-9]+

%state TOKEN_STATE
%state WAITING_FOR_TOKEN_STATE

%%

<YYINITIAL> {
	{Command} {
		command = new Command();
		command.commandName = yytext();
		yybegin(WAITING_FOR_TOKEN_STATE);
	}
	[^] {
		return null;
	}
	<<EOF>> {
		return null;
	}
}

<WAITING_FOR_TOKEN_STATE> {
	{EOL} {
		yybegin(YYINITIAL);
		return command;
	}
	[\[] {
		yybegin(TOKEN_STATE);
		currentToken = "";
		bracketCounter = 1;
	}
	[^] {
	}
	<<EOF>> {
		yybegin(YYINITIAL);
		return command;
	}
}


<TOKEN_STATE> {
	"\\\\" {
		currentToken = currentToken + "\\";
	}
	"\\]" {
		currentToken = currentToken + "]";
	}
	"\\[" {
		currentToken = currentToken + "[";
	}
	[\]] {
		bracketCounter--;
		if (bracketCounter == 0) {
			yybegin(WAITING_FOR_TOKEN_STATE);
			command.tokens.add(currentToken);
		} else
			currentToken = currentToken + "]";
	}
	[\[] {
		bracketCounter++;
		currentToken = currentToken + "[";
	}
	[^] {
		currentToken = currentToken + yytext();
	}
	<<EOF>> {
		yybegin(YYINITIAL);
		command.tokens.add(currentToken);
		return command;
	}
}