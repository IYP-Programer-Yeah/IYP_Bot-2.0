package botelements.messageelements;

import javafx.util.Pair;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by HoseinGhahremanzadeh on 7/11/2017.
 */
public class BotMessage implements Serializable{
    public ArrayList<MessagePart> messageParts = new ArrayList<MessagePart>();
    public String message;
    public String error = "";
    public String warnings = "";
    public String author;
    public boolean isTestFeature = true;
    public boolean isInterrupting = true;
    public boolean caseSensitive = false;


    public boolean isValid() {
        boolean isValid = true;
        for (MessagePart messagePart : messageParts)
            if (!messagePart.isValid()) {
                error = error + messagePart.error;
                isValid = false;
            }
        return isValid;
    }

    public void normalizeResources() {
        for (int i = 0; i < messageParts.size(); i++)
            if (messageParts.get(i).type == MessagePart.MessagePartType.Resource && messageParts.get(i).resource.count > 0){
                for (int j = 1; j < messageParts.get(i).resource.count; j++) {
                    messageParts.add(i, messageParts.get(i));
                    i++;
                }
                messageParts.get(i).resource.count = 1;
            } else if (messageParts.get(i).type == MessagePart.MessagePartType.Program)
                messageParts.get(i).program.normalizeArguments();
    }

    public int findPreviousVariablesSizeMessagePart (int i) {
        i--;
        while (i >= 0 && messageParts.get(i).type != MessagePart.MessagePartType.Program && messageParts.get(i).type != MessagePart.MessagePartType.Resource)
            i--;
        return i;
    }
    public ArrayList<Pair<Integer, ResourceMessagePart>> getAvailableResources() {
        ArrayList<Pair<Integer, ResourceMessagePart>> availableResources = new ArrayList<>();

        for (int i=0; i<messageParts.size(); i++)
            if (messageParts.get(i).type == MessagePart.MessagePartType.Resource && messageParts.get(i).resource.id != -1)
                availableResources.add(new Pair<Integer, ResourceMessagePart>(i, messageParts.get(i).resource));
        return availableResources;
    }

    public Pair<Boolean, Integer> evaluateProgramsArgumentResourceIndices(ArrayList<Pair<Integer, ResourceMessagePart>> availableResources) {
        boolean[] isInUse = new boolean[availableResources.size()];
        Pair<Boolean, Integer> programResult;
        boolean perfect = true;
        int danglingResourceCount = 0;

        for (int i=0; i<messageParts.size(); i++)
            if (messageParts.get(i).type == MessagePart.MessagePartType.Program) {
                programResult = messageParts.get(i).program.evaluateArgumentResourceIndices(availableResources, isInUse);
                if (!programResult.getKey())
                    warnings = warnings + "\n Program: ``" + messageParts.get(i).program.className + "." + messageParts.get(i).program.functionName + "`` does not have a perfect argument match.";
                if (programResult.getValue() != 0)
                    error = error + "\n Program: ``" + messageParts.get(i).program.className + "." + messageParts.get(i).program.functionName + "`` does not have a viable argument match, there are ``" + programResult.getValue() + "`` dangling resource reference" + (programResult.getValue() > 1?"s.":".");

                perfect = perfect && programResult.getKey();
                danglingResourceCount += programResult.getValue();
            }
        return new Pair<>(perfect, danglingResourceCount);
    }

    public Pair<Boolean, Integer> evaluateReferencedResourceIndices(ArrayList<Pair<Integer, ResourceMessagePart>> availableResources) {
        boolean[] isInUse = new boolean[availableResources.size()];

        boolean isPerfect = true;
        int danglingResourceCount = 0;

        for (MessagePart messagePart : messageParts)
            if (messagePart.type == MessagePart.MessagePartType.Resource) {
                Pair<Boolean, Boolean> messagePartResult =  messagePart.resource.evaluateResourceIndex(availableResources, isInUse);
                if (!messagePartResult.getValue())
                    danglingResourceCount++;
                isPerfect = isPerfect && messagePartResult.getKey();
            } else if (messagePart.type == MessagePart.MessagePartType.Program) {
                Pair<Boolean, Integer> messagePartResult =  messagePart.program.evaluateArgumentResourceIndices(availableResources, isInUse);
                danglingResourceCount += messagePartResult.getValue();
                isPerfect = isPerfect && messagePartResult.getKey();
            }
        return new Pair<>(isPerfect, danglingResourceCount);
    }

    public boolean hasForwardReferencing() {
        boolean hasForwardReferencing = false;
        for (int i=0; i<messageParts.size(); i++)
            if (messageParts.get(i).type == MessagePart.MessagePartType.Program && messageParts.get(i).program.checkForwardReferencing(i)) {
                error = error + "\n Program: ``" + messageParts.get(i).program.className + "." + messageParts.get(i).program.functionName + "`` has reference to resources that comes after it, forward referencing is not supported, move the program to end and use a * resource to achieve intended behaviour.";
                hasForwardReferencing = true;
            }
        return hasForwardReferencing;
    }
}
