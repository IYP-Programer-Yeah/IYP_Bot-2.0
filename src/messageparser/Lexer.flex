package messageparser;

import botelements.*;
import botelements.messageelements.*;
import java.util.ArrayList;

%%
%class MessageScanner

%unicode
%line
%column
%public
%type BotMessage
%{
	private BotMessage message = new BotMessage();
	private MessagePart currentPart= new MessagePart();
	private ResourceMessagePart currentResource;
	private ProgramMessagePart currentProgram;
	
	private int resourceRetrunState;
	
    public int getLine() {
        return yyline + 1;
    }
	
	public int getColumn() {
		return yycolumn + 1;
	}
%}

EOL = \r|\n|\r\n
WhiteSpace = {EOL}|\t|" "

RawMessage = [^]

BotMentionTag = [\@][Bb]ot
SenderMentionTag = [\@][Ss]ender
MentionTag = {BotMentionTag}|{SenderMentionTag}

IdentifierDigitAndLetter = [a-zA-Z0-9]
IdentifierRepeatingBlock = [_]+{IdentifierDigitAndLetter}+
Identifier = ([a-zA-Z]+{IdentifierDigitAndLetter}*){IdentifierRepeatingBlock}*| {IdentifierRepeatingBlock}+

DecimalDigit = [0-9]
DecimalIntegerConstant = [+-]?{DecimalDigit}+
PositiveDecimalIntegerConstant = [+]?{DecimalDigit}+

%state RESOURCE_STATE
%state RESOURCE_ID_STATE
%state RESOURCE_COUNT_STATE

%state PROGRAM_CLASS_STATE
%state PROGRAM_FUNCTION_STATE
%state PROGRAM_ARGUMENT_STATE


%%

<YYINITIAL> {
	{MentionTag} {
		message.messageParts.add(currentPart);
		currentPart = new MessagePart();
		currentPart.rawMessage = yytext().toLowerCase();
		currentPart.type = MessagePart.MessagePartType.MentionTag;
		message.messageParts.add(currentPart);
		currentPart = new MessagePart();
	}
	"\\<" {
		currentPart.rawMessage = currentPart.rawMessage + "<";
	}
	"\\~" {
		currentPart.rawMessage = currentPart.rawMessage + "~";
	}
	"\\{" {
		currentPart.rawMessage = currentPart.rawMessage + "{";
	}
	[\<] {
		message.messageParts.add(currentPart);
		currentPart = new MessagePart();
		currentPart.resource = new ResourceMessagePart();
		currentPart.type = MessagePart.MessagePartType.Resource;
		currentResource = currentPart.resource;
		resourceRetrunState = YYINITIAL;
		yybegin(RESOURCE_STATE);
		//further process on the currentResource
		message.messageParts.add(currentPart);
		currentPart = new MessagePart();
	}
	[\~] {
		message.messageParts.add(currentPart);
		currentPart = new MessagePart();
		currentPart.program = new ProgramMessagePart();
		currentProgram = currentPart.program;
		currentPart.type = MessagePart.MessagePartType.Program;
		yybegin(PROGRAM_CLASS_STATE);
		//further process on the currentProgram
		message.messageParts.add(currentPart);
		currentPart = new MessagePart();
	}
	{RawMessage} {
		currentPart.rawMessage = currentPart.rawMessage + yytext();
	}
	<<EOF>> {
		message.messageParts.add(currentPart);
		return message;
	}
}

<RESOURCE_STATE> {
	"\\>" {
		currentResource.name = currentResource.name + ">";
	}
	">{" {
		yybegin(RESOURCE_ID_STATE);
	}
	[\>] {
		yybegin(resourceRetrunState);
	}
	{RawMessage} {
		currentResource.name = currentResource.name + yytext();
	}
	<<EOF>> {
		yybegin(resourceRetrunState);
	}
}

<RESOURCE_ID_STATE> {
	"}{" {
		yybegin(RESOURCE_COUNT_STATE);
	}
	[\}] {
		yybegin(resourceRetrunState);
	}
	<<EOF>> {
		yybegin(resourceRetrunState);
	}
	{DecimalIntegerConstant} {
		currentResource.id = Integer.parseInt(yytext());
	}
	[^] {
	}
}

<RESOURCE_COUNT_STATE> {
	[\}] {
		yybegin(resourceRetrunState);
	}
	<<EOF>> {
		yybegin(resourceRetrunState);
	}
	{PositiveDecimalIntegerConstant} {
		currentResource.count = Integer.parseInt(yytext());
	}
	[\*] {
		currentResource.count = -1;
	}
	[\+] {
		currentResource.count = -2;
	}
	[^] {
	}
}



<PROGRAM_CLASS_STATE> {
	{WhiteSpace} {
	}
	{Identifier} {
		currentProgram.className = yytext();
	}
	[\.] {
		yybegin(PROGRAM_FUNCTION_STATE);
	}
	[^] {
		yybegin(YYINITIAL);
	}
	<<EOF>> {
		yybegin(YYINITIAL);
	}
}

<PROGRAM_FUNCTION_STATE> {
	{WhiteSpace} {
	}
	{Identifier} {
		currentProgram.functionName = yytext();
	}
	[\(] {
		yybegin(PROGRAM_ARGUMENT_STATE);
	}
	[^] {
		yybegin(YYINITIAL);
	}
	<<EOF>> {
		yybegin(YYINITIAL);
	}
}

<PROGRAM_ARGUMENT_STATE> {
	{WhiteSpace} {
	}
	[\<] {
		currentResource = new ResourceMessagePart();
		currentProgram.arguments.add(currentResource);
		resourceRetrunState = PROGRAM_ARGUMENT_STATE;
		yybegin(RESOURCE_STATE);
	}
	[\)] {
		yybegin(YYINITIAL);
	}
	[\,] {
	}
	[^] {
		yybegin(YYINITIAL);
	}
	<<EOF>> {
		yybegin(YYINITIAL);
	}
}