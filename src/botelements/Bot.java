package botelements;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


import utility.JavaSourceCompiler;
import utility.NFA;
import utility.Trie;
import botelements.messageelements.BotMessage;
import botelements.messageelements.MessagePart;
import botelements.messageelements.ProgramMessagePart;
import botelements.messageelements.ResourceMessagePart;
import com.google.common.util.concurrent.FutureCallback;
import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.Javacord;
import de.btobastian.javacord.entities.User;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.entities.permissions.Role;
import de.btobastian.javacord.listener.message.MessageCreateListener;
import javafx.util.Pair;
import messageparser.MessageScanner;
import scanner.Scanner;

/**
 * Created by HoseinGhahremanzadeh on 7/11/2017.
 */
public class Bot {
    private String saveDirectory;
    private DiscordAPI api;

    private HashSet<Command> commandSet = new HashSet<Command>();

    private HashMap<String, ArrayList<BotMessage>> messages = new HashMap<String, ArrayList<BotMessage>>();
    private HashMap<String, ArrayList<BotMessage>> responses = new HashMap<String, ArrayList<BotMessage>>();
    private HashMap<String, Trie> resourcesCS = new HashMap<String, Trie>();
    private HashMap<String, Trie> resourcesCI = new HashMap<String, Trie>();
    private HashMap<String, String> programs = new HashMap<String, String>();


    private String botMentionTag;

    private HashMap<String, Integer> channelsPermission = new HashMap<String, Integer>();
    private HashMap<String, Integer> rolesPermission = new HashMap<String, Integer>();
    private HashMap<String, Integer> membersPermission = new HashMap<String, Integer>();
    private HashMap<String, Boolean> notifiedMembers = new HashMap<String, Boolean>();
    private HashSet<String> testChannels = new HashSet<>();


    public void savePermissions(){
        File permissions=new File("permissions");

        try {
            ObjectOutputStream objectOutputStream=new ObjectOutputStream(new FileOutputStream(permissions));

            objectOutputStream.writeObject(channelsPermission);
            objectOutputStream.writeObject(rolesPermission);
            objectOutputStream.writeObject(membersPermission);
            objectOutputStream.writeObject(notifiedMembers);
            objectOutputStream.writeObject(testChannels);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadPermissions(){
        File permissions=new File("permissions");
        if (permissions.exists()) {
            try {
                ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(permissions));

                channelsPermission = (HashMap<String, Integer>)objectInputStream.readObject();
                rolesPermission = (HashMap<String, Integer>)objectInputStream.readObject();
                membersPermission = (HashMap<String, Integer>)objectInputStream.readObject();
                notifiedMembers = (HashMap<String, Boolean>)objectInputStream.readObject();
                testChannels = (HashSet<String>)objectInputStream.readObject();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean saveDatabase (String saveName) {
        File database=new File(saveDirectory + saveName);

        try {
            ObjectOutputStream objectOutputStream=new ObjectOutputStream(new FileOutputStream(database));

            objectOutputStream.writeObject(messages);
            objectOutputStream.writeObject(responses);
            objectOutputStream.writeObject(resourcesCS);
            objectOutputStream.writeObject(resourcesCI);
            objectOutputStream.writeObject(programs);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean loadDatabase (String saveName) {
        File database=new File(saveDirectory + saveName);

        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(database));

            messages = (HashMap<String, ArrayList<BotMessage>>) objectInputStream.readObject();
            responses = (HashMap<String, ArrayList<BotMessage>>) objectInputStream.readObject();
            resourcesCS = (HashMap<String, Trie>) objectInputStream.readObject();
            resourcesCI = (HashMap<String, Trie>) objectInputStream.readObject();
            programs = (HashMap<String, String>) objectInputStream.readObject();

            for (Map.Entry<String, String> program:programs.entrySet())
                JavaSourceCompiler.compileString(program.getValue(), program.getKey());

            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void resetDatabase () {
        messages = new HashMap<String, ArrayList<BotMessage>>();
        responses = new HashMap<String, ArrayList<BotMessage>>();
        resourcesCS = new HashMap<String, Trie>();
        resourcesCI = new HashMap<String, Trie>();
        programs = new HashMap<String, String>();
    }

    public boolean mergeDatabase (String saveName) {
        File database=new File(saveDirectory + saveName);

        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(database));

            HashMap<String, ArrayList<BotMessage>> messages = (HashMap<String, ArrayList<BotMessage>>) objectInputStream.readObject();
            HashMap<String, ArrayList<BotMessage>>responses = (HashMap<String, ArrayList<BotMessage>>) objectInputStream.readObject();
            HashMap<String, Trie> resources = (HashMap<String, Trie>) objectInputStream.readObject();
            HashMap<String, String> programs = (HashMap<String, String>) objectInputStream.readObject();

            for (Map.Entry<String, String> program:programs.entrySet())
                JavaSourceCompiler.compileString(program.getValue(), program.getKey());

            this.programs.putAll(programs);

            for (Map.Entry<String, Trie> entry:resources.entrySet())
                if (this.resourcesCS.containsKey(entry.getKey()))
                    for (String key:resources.get(entry.getKey()).getKeys())
                        this.resourcesCS.get(entry.getKey()).put(key, true);
                else
                    this.resourcesCS.put(entry.getKey(), entry.getValue());

            for (Map.Entry<String, Trie> entry:resources.entrySet())
                if (this.resourcesCI.containsKey(entry.getKey()))
                    for (String key:resources.get(entry.getKey()).getKeys())
                        this.resourcesCI.get(entry.getKey()).put(key, true);
                else
                    this.resourcesCI.put(entry.getKey(), entry.getValue());

            for (Map.Entry<String, ArrayList<BotMessage>> entry:messages.entrySet())
                if (this.messages.containsKey(entry.getKey()))
                    this.messages.get(entry.getKey()).addAll(entry.getValue());
                else
                    this.messages.put(entry.getKey(), entry.getValue());

            for (Map.Entry<String, ArrayList<BotMessage>> entry:responses.entrySet())
                if (this.responses.containsKey(entry.getKey()))
                    this.responses.get(entry.getKey()).addAll(entry.getValue());
                else
                    this.responses.put(entry.getKey(), entry.getValue());

            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private int acquirePermission(Message message) {
        if (membersPermission.size() == 0) {
            membersPermission.put(message.getAuthor().getMentionTag(), Command.ADMIN);
            savePermissions();
        }

        int userPermission = Command.NONE;
        int rolePermission = Command.NONE;
        int channelPermission = Command.NONE;

        String authorMentionTag = message.getAuthor().getMentionTag();
        String channelMentionTag = authorMentionTag;
        try {
            channelMentionTag = message.getChannelReceiver().getMentionTag();
        } catch (Exception E) {}

        if (membersPermission.containsKey(authorMentionTag))
            userPermission = membersPermission.get(authorMentionTag);
        if (channelsPermission.containsKey(channelMentionTag))
            channelPermission = channelsPermission.get(channelMentionTag);

        if (!authorMentionTag.equals(channelMentionTag)) {
            Collection<Role> rolesCollection = message.getAuthor().getRoles(message.getChannelReceiver().getServer());

            for (Role userRole : rolesCollection) {
                int currentRole = rolePermission;
                if (rolesPermission.containsKey(userRole.getId()))
                    currentRole = rolesPermission.get(userRole.getId());
                if (rolePermission == Command.NONE && currentRole == Command.BLACK)
                    rolePermission = Command.BLACK;

                rolePermission = Math.max(currentRole, rolePermission);
            }
        }

        int permission = userPermission;

        if (permission == Command.NONE)
            permission = rolePermission;

        if (permission != Command.ADMIN)
            permission = Math.min(permission, channelPermission);
        return permission;
    }

    private ArrayList<Command> acquireCommands(Message message) {
        ArrayList<Command> commands = new ArrayList<Command>();
        int permission = acquirePermission(message);
        Scanner scanner = new Scanner(new StringReader(message.getContent()));
        Command command = null;
        try {
            command = scanner.yylex();
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (command != null) {
            command.permission = permission;
            boolean messageRecognized = false;
            for (Command functionalCommand : commandSet)
                if (command.equals(functionalCommand)) {
                    command.function = functionalCommand.function;
                    commands.add(command);
                    messageRecognized = true;
                    break;
                }
            if (!messageRecognized)
                break;
            try {
                command = scanner.yylex();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return commands;
    }

    private void setUpCommandSet() {
        Command commandToAdd;


        //.Permission
        commandToAdd = new Command();
        commandToAdd.commandName = ".Permission";
        commandToAdd.permission = Command.ADMIN;
        commandToAdd.tokens.add("");
        commandToAdd.tokens.add("");
        commandToAdd.tokens.add("");
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                int permission;
                switch (command.tokens.get(1).replace(" ", "").toLowerCase()) {
                    case "black": {
                        permission = Command.BLACK;
                        break;
                    }
                    case "white": {
                        permission = Command.WHITE;
                        break;
                    }
                    case "admin": {
                        permission = Command.ADMIN;
                        break;
                    }
                    case "none": {
                        permission = Command.NONE;
                        break;
                    }
                    default: {
                        message.getChannelReceiver().sendMessage("Unknown permission: " + command.tokens.get(1));
                        return;
                    }
                }
                switch (command.tokens.get(0).replace(" ", "").toLowerCase()) {
                    case "channel": {
                        if (permission == Command.ADMIN) {
                            message.reply("Channels can't be admins.");
                            return;
                        } else {
                            if (permission != Command.NONE)
                                channelsPermission.put(command.tokens.get(2).replace(" ", ""), permission);
                            else
                                channelsPermission.remove(command.tokens.get(2).replace(" ", ""));
                        }
                        break;
                    }
                    case "member": {
                        if (permission != Command.NONE)
                            membersPermission.put(command.tokens.get(2).replace(" ", ""), permission);
                        else
                            membersPermission.remove(command.tokens.get(2).replace(" ", ""));
                        break;
                    }
                    case "role": {
                        if (permission != Command.NONE)
                            rolesPermission.put(command.tokens.get(2).replace(" ", ""), permission);
                        else
                            rolesPermission.remove(command.tokens.get(2).replace(" ", ""));
                        break;
                    }
                    default: {
                        message.getChannelReceiver().sendMessage("Unknown permission receiver: " + command.tokens.get(0));
                        return;
                    }
                }
                savePermissions();
                message.reply("Permission applied.");
            }
        };
        commandSet.add(commandToAdd);

        //.NewCat
        commandToAdd = new Command();
        commandToAdd.commandName = ".NewCat";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.tokens.add("");
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (messages.containsKey(command.tokens.get(0)) && responses.containsKey(command.tokens.get(0))) {
                    message.reply("Category already exists.");
                } else {
                    responses.put(command.tokens.get(0), new ArrayList<BotMessage>());
                    messages.put(command.tokens.get(0), new ArrayList<BotMessage>());
                    message.reply("Category added.");
                }
            }
        };
        commandSet.add(commandToAdd);

        //.NewRes
        commandToAdd = new Command();
        commandToAdd.commandName = ".NewRes";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.tokens.add("");
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (resourcesCS.containsKey(command.tokens.get(0))) {
                    message.reply("Resource already exists.");
                } else {
                    resourcesCS.put(command.tokens.get(0), new Trie());
                    resourcesCI.put(command.tokens.get(0), new Trie());
                    message.reply("Resource added.");
                }
            }
        };
        commandSet.add(commandToAdd);

        //.RemoveCat
        commandToAdd = new Command();
        commandToAdd.commandName = ".RemoveCat";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.tokens.add("");
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (messages.containsKey(command.tokens.get(0)) && responses.containsKey(command.tokens.get(0))) {
                    responses.remove(command.tokens.get(0));
                    messages.remove(command.tokens.get(0));
                    message.reply("Category removed.");
                } else
                    message.reply("Category does't exist.");
            }
        };
        commandSet.add(commandToAdd);

        //.RemoveRes
        commandToAdd = new Command();
        commandToAdd.commandName = ".RemoveRes";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.tokens.add("");
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (resourcesCS.containsKey(command.tokens.get(0))) {
                    resourcesCS.remove(command.tokens.get(0));
                    resourcesCI.remove(command.tokens.get(0));
                    message.reply("Resource removed.");
                } else
                    message.reply("Resource doesn't exist.");
            }
        };
        commandSet.add(commandToAdd);

        //.AddToRes
        commandToAdd = new Command();
        commandToAdd.commandName = ".AddToRes";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.tokens.add("");
        commandToAdd.tokens.add("");
        commandToAdd.noUpperBoundForTokenCount = true;
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (resourcesCS.containsKey(command.tokens.get(0))) {
                    for (int i=1;i<command.tokens.size();i++) {
                        resourcesCS.get(command.tokens.get(0)).put(command.tokens.get(i), true);
                        resourcesCI.get(command.tokens.get(0)).put(command.tokens.get(i).toLowerCase(), true);
                    }
                    message.reply("Added to resource.");
                } else
                    message.reply("Resource doesn't exist.");
            }
        };
        commandSet.add(commandToAdd);

        //.RemoveFromRes
        commandToAdd = new Command();
        commandToAdd.commandName = ".RemoveFromRes";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.tokens.add("");
        commandToAdd.tokens.add("");
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (resourcesCS.containsKey(command.tokens.get(0))) {
                    if (resourcesCS.get(command.tokens.get(0)).remove(command.tokens.get(1)) == null) {
                        message.reply("Element did not exist in the resource.");
                        return;
                    }
                    message.reply("Removed from resource.");
                    String resourceCI = command.tokens.get(1).toLowerCase();
                    for (String element : resourcesCS.get(command.tokens.get(0)).getKeys())
                        if (element.toLowerCase().equals(resourceCI))
                            return;
                    resourcesCI.remove(resourceCI);
                } else
                    message.reply("Resource doesn't exist.");
            }
        };
        commandSet.add(commandToAdd);

        //.ListRess
        commandToAdd = new Command();
        commandToAdd.commandName = ".ListRess";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (resourcesCS.isEmpty()) {
                    message.reply("There are no resources.");
                } else {
                    String keys = "List of resources:";
                    for (String key : resourcesCS.keySet())
                        keys = keys + "\n" + key;
                    message.reply(keys);
                }
            }
        };
        commandSet.add(commandToAdd);

        //.ListCats
        commandToAdd = new Command();
        commandToAdd.commandName = ".ListCats";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (messages.isEmpty()) {
                    message.reply("There are no categories.");
                } else {
                    String keys = "List of categories:";
                    for (String key : messages.keySet())
                        keys = keys + "\n" + key;
                    message.reply(keys);
                }
            }
        };
        commandSet.add(commandToAdd);

        //.ListResElements
        commandToAdd = new Command();
        commandToAdd.commandName = ".ListResElements";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.tokens.add("");
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (resourcesCS.containsKey(command.tokens.get(0))) {
                    if (resourcesCS.get(command.tokens.get(0)).getKeys().size() != 0){
                        String keys = "Resource ``" + command.tokens.get(0) + "`` contains elements:";
                        for (String key : resourcesCS.get(command.tokens.get(0)).getKeys())
                            keys = keys + "\n" + key;
                        message.reply(keys);
                    } else
                        message.reply("Resource ``" + command.tokens.get(0) + "`` contains no elements.");
                } else
                    message.reply("Resource ``" + command.tokens.get(0) + "`` doesn't exist.");
            }
        };
        commandSet.add(commandToAdd);

        //.ListMessages
        commandToAdd = new Command();
        commandToAdd.commandName = ".ListMessages";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.tokens.add("");
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (messages.containsKey(command.tokens.get(0))) {
                    ArrayList<BotMessage> catMessages= messages.get(command.tokens.get(0));
                    if (catMessages.size() != 0){
                        String result = "Category  ``" + command.tokens.get(0) + "`` contains messages:";
                        for (int i=0; i<catMessages.size(); i++)
                            result = result + "\n" + i + ": " + catMessages.get(i).message;
                        message.reply(result);
                    } else
                        message.reply("Category ``" + command.tokens.get(0) + "`` contains no messages.");
                } else
                    message.reply("Category ``" + command.tokens.get(0) + "`` doesn't exist.");
            }
        };
        commandSet.add(commandToAdd);

        //.ListResponses
        commandToAdd = new Command();
        commandToAdd.commandName = ".ListResponses";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.tokens.add("");
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (responses.containsKey(command.tokens.get(0))) {
                    ArrayList<BotMessage> catResponses= responses.get(command.tokens.get(0));
                    if (catResponses.size() != 0){
                        String result = "Category  " + command.tokens.get(0) + " contains responses:";
                        for (int i=0; i<catResponses.size(); i++)
                            result = result + "\n" + i + ": " + catResponses.get(i).message;
                        message.reply(result);
                    } else
                        message.reply("Category ``" + command.tokens.get(0) + "`` contains no responses.");
                } else
                    message.reply("Category ``" + command.tokens.get(0) + "`` doesn't exist.");
            }
        };
        commandSet.add(commandToAdd);

        //.RemoveMessage
        commandToAdd = new Command();
        commandToAdd.commandName = ".RemoveMessage";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.tokens.add("");
        commandToAdd.tokens.add("");
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (messages.containsKey(command.tokens.get(0))) {
                    ArrayList<BotMessage> catMessages= messages.get(command.tokens.get(0));
                    int index = Integer.parseInt(command.tokens.get(1));
                    if (catMessages.size() > index && index >= 0) {
                        message.reply("Message #" + command.tokens.get(1) + ": ``" + catMessages.remove(index).message + "`` removed successfully from category: ``" + command.tokens.get(0) + "``");
                    } else if (catMessages.size() == 0)
                        message.reply("Category ``" + command.tokens.get(0) + "`` contains no messages.");
                    else
                        message.reply("Index out of bounds.");
                } else
                    message.reply("Category ``" + command.tokens.get(0) + "`` doesn't exist.");
            }
        };
        commandSet.add(commandToAdd);

        //.RemoveResponse
        commandToAdd = new Command();
        commandToAdd.commandName = ".RemoveResponse";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.tokens.add("");
        commandToAdd.tokens.add("");
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (responses.containsKey(command.tokens.get(0))) {
                    ArrayList<BotMessage> catResponses= responses.get(command.tokens.get(0));
                    int index = Integer.parseInt(command.tokens.get(1));
                    if (catResponses.size() > index && index >= 0) {
                        message.reply("Message #" + command.tokens.get(1) + ": ``" + catResponses.remove(index).message + "`` removed successfully from category: ``" + command.tokens.get(0) + "``");
                    } else if (catResponses.size() == 0)
                        message.reply("Category ``" + command.tokens.get(0) + "`` contains no responses.");
                    else
                        message.reply("Index out of bounds.");
                } else
                    message.reply("Category ``" + command.tokens.get(0) + "`` doesn't exist.");
            }
        };
        commandSet.add(commandToAdd);

        //.EditMessage
        commandToAdd = new Command();
        commandToAdd.commandName = ".EditMessage";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.tokens.add("");
        commandToAdd.tokens.add("");
        commandToAdd.tokens.add("");
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (messages.containsKey(command.tokens.get(0))) {
                    ArrayList<BotMessage> catMessages= messages.get(command.tokens.get(0));
                    int index = Integer.parseInt(command.tokens.get(1));
                    if (catMessages.size() > index && index >= 0) {


                        MessageScanner messageScanner = new MessageScanner(new StringReader(command.tokens.get(2)));
                        try {
                            BotMessage botMessage = messageScanner.yylex();
                            if (message == null) {
                                message.reply("Something is terribly wrong with your message");
                                return;
                            }
                            if (!botMessage.isValid()) {
                                message.reply("The message contains following errors:" + botMessage.error);
                                return;
                            }

                            botMessage.normalizeResources();

                            Pair<Boolean, Integer> argumentEvaluationResult =  botMessage.evaluateProgramsArgumentResourceIndices(botMessage.getAvailableResources());

                            if (botMessage.hasForwardReferencing() || argumentEvaluationResult.getValue() != 0) {
                                message.reply("The message contains following errors:" + botMessage.error);
                                return;
                            }

                            botMessage.message = command.tokens.get(1);
                            botMessage.author = message.getAuthor().getId();
                            catMessages.set(index, botMessage);
                            if (botMessage.warnings.length() == 0)
                                message.reply("Message successfully edited.");
                            else
                                message.reply("Message successfully edited with warnings:" + botMessage.warnings);
                        } catch (IOException e) {
                            message.reply("Something went wrong, no idea what.");
                        }



                    } else if (catMessages.size() == 0)
                        message.reply("Category ``" + command.tokens.get(0) + "`` contains no messages.");
                    else
                        message.reply("Index out of bounds.");
                } else
                    message.reply("Category ``" + command.tokens.get(0) + "`` doesn't exist.");
            }
        };
        commandSet.add(commandToAdd);


        //.EditMessage
        commandToAdd = new Command();
        commandToAdd.commandName = ".EditMessage";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.tokens.add("");
        commandToAdd.tokens.add("");
        commandToAdd.tokens.add("");
        commandToAdd.tokens.add("");
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                String caseToken = command.tokens.get(3).toLowerCase().replace(" ","");
                if (!caseToken.toLowerCase().equals("true") && !caseToken.toLowerCase().equals("false")) {
                    message.reply("The third operand should be either true, or false.");
                    return;
                }
                if (messages.containsKey(command.tokens.get(0))) {
                    ArrayList<BotMessage> catMessages= messages.get(command.tokens.get(0));
                    int index = Integer.parseInt(command.tokens.get(1));
                    if (catMessages.size() > index && index >= 0) {


                        MessageScanner messageScanner = new MessageScanner(new StringReader(command.tokens.get(2)));
                        try {
                            BotMessage botMessage = messageScanner.yylex();
                            if (message == null) {
                                message.reply("Something is terribly wrong with your message");
                                return;
                            }
                            if (!botMessage.isValid()) {
                                message.reply("The message contains following errors:" + botMessage.error);
                                return;
                            }

                            botMessage.normalizeResources();

                            Pair<Boolean, Integer> argumentEvaluationResult =  botMessage.evaluateProgramsArgumentResourceIndices(botMessage.getAvailableResources());

                            if (botMessage.hasForwardReferencing() || argumentEvaluationResult.getValue() != 0) {
                                message.reply("The message contains following errors:" + botMessage.error);
                                return;
                            }

                            botMessage.message = command.tokens.get(1);
                            botMessage.author = message.getAuthor().getId();
                            catMessages.set(index, botMessage);
                            if (botMessage.warnings.length() == 0)
                                message.reply("Message successfully edited.");
                            else
                                message.reply("Message successfully edited with warnings:" + botMessage.warnings);
                            botMessage.caseSensitive = caseToken.equals("true");
                        } catch (IOException e) {
                            message.reply("Something went wrong, no idea what.");
                        }



                    } else if (catMessages.size() == 0)
                        message.reply("Category ``" + command.tokens.get(0) + "`` contains no messages.");
                    else
                        message.reply("Index out of bounds.");
                } else
                    message.reply("Category ``" + command.tokens.get(0) + "`` doesn't exist.");
            }
        };
        commandSet.add(commandToAdd);

        //.EditResponse
        commandToAdd = new Command();
        commandToAdd.commandName = ".EditResponse";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.tokens.add("");
        commandToAdd.tokens.add("");
        commandToAdd.tokens.add("");
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (responses.containsKey(command.tokens.get(0))) {
                    ArrayList<BotMessage> catResponses= responses.get(command.tokens.get(0));
                    int index = Integer.parseInt(command.tokens.get(1));
                    if (catResponses.size() > index && index >= 0) {


                        MessageScanner messageScanner = new MessageScanner(new StringReader(command.tokens.get(2)));
                        try {
                            BotMessage botMessage = messageScanner.yylex();
                            if (message == null) {
                                message.reply("Something is terribly wrong with your response");
                                return;
                            }
                            if (!botMessage.isValid()) {
                                message.reply("The response contains following errors:" + botMessage.error);
                                return;
                            }

                            botMessage.message = command.tokens.get(2);
                            botMessage.normalizeResources();
                            botMessage.author = message.getAuthor().getId();
                            catResponses.set(index, botMessage);
                            message.reply("Response successfully edited.");
                        } catch (IOException e) {
                            message.reply("Something went wrong, no idea what.");
                        }



                    } else if (catResponses.size() == 0)
                        message.reply("Category ``" + command.tokens.get(0) + "`` contains no responses.");
                    else
                        message.reply("Index out of bounds.");
                } else
                    message.reply("Category ``" + command.tokens.get(0) + "`` doesn't exist.");
            }
        };
        commandSet.add(commandToAdd);


        //.AddMessage
        commandToAdd = new Command();
        commandToAdd.commandName = ".AddMessage";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.tokens.add("");
        commandToAdd.tokens.add("");
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (messages.containsKey(command.tokens.get(0))) {
                    ArrayList<BotMessage> catMessages= messages.get(command.tokens.get(0));
                    MessageScanner messageScanner = new MessageScanner(new StringReader(command.tokens.get(1)));
                    try {
                        BotMessage botMessage = messageScanner.yylex();
                        if (message == null) {
                            message.reply("Something is terribly wrong with your message");
                            return;
                        }
                        if (!botMessage.isValid()) {
                            message.reply("The message contains following errors:" + botMessage.error);
                            return;
                        }

                        botMessage.normalizeResources();

                        Pair<Boolean, Integer> argumentEvaluationResult =  botMessage.evaluateProgramsArgumentResourceIndices(botMessage.getAvailableResources());

                        if (botMessage.hasForwardReferencing() || argumentEvaluationResult.getValue() != 0) {
                            message.reply("The message contains following errors:" + botMessage.error);
                            return;
                        }

                        botMessage.message = command.tokens.get(1);
                        botMessage.author = message.getAuthor().getId();
                        catMessages.add(botMessage);
                        if (botMessage.warnings.length() == 0)
                            message.reply("Message successfully added.");
                        else
                            message.reply("Message successfully added with warnings:" + botMessage.warnings);
                    } catch (IOException e) {
                        message.reply("Something went wrong, no idea what.");
                    }
                } else
                    message.reply("Category ``" + command.tokens.get(0) + "`` doesn't exist.");
            }
        };
        commandSet.add(commandToAdd);


        //.AddMessage
        commandToAdd = new Command();
        commandToAdd.commandName = ".AddMessage";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.tokens.add("");
        commandToAdd.tokens.add("");
        commandToAdd.tokens.add("");
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                String caseToken = command.tokens.get(2).toLowerCase().replace(" ","");
                if (!caseToken.toLowerCase().equals("true") && !caseToken.toLowerCase().equals("false")) {
                    message.reply("The third operand should be either true, or false.");
                    return;
                }
                if (messages.containsKey(command.tokens.get(0))) {
                    ArrayList<BotMessage> catMessages= messages.get(command.tokens.get(0));
                    MessageScanner messageScanner = new MessageScanner(new StringReader(command.tokens.get(1)));
                    try {
                        BotMessage botMessage = messageScanner.yylex();
                        if (message == null) {
                            message.reply("Something is terribly wrong with your message");
                            return;
                        }
                        if (!botMessage.isValid()) {
                            message.reply("The message contains following errors:" + botMessage.error);
                            return;
                        }

                        botMessage.normalizeResources();

                        Pair<Boolean, Integer> argumentEvaluationResult =  botMessage.evaluateProgramsArgumentResourceIndices(botMessage.getAvailableResources());

                        if (botMessage.hasForwardReferencing() || argumentEvaluationResult.getValue() != 0) {
                            message.reply("The message contains following errors:" + botMessage.error);
                            return;
                        }

                        botMessage.message = command.tokens.get(1);
                        botMessage.author = message.getAuthor().getId();
                        catMessages.add(botMessage);
                        if (botMessage.warnings.length() == 0)
                            message.reply("Message successfully added.");
                        else
                            message.reply("Message successfully added with warnings:" + botMessage.warnings);
                        botMessage.caseSensitive = caseToken.equals("true");
                    } catch (IOException e) {
                        message.reply("Something went wrong, no idea what.");
                    }
                } else
                    message.reply("Category ``" + command.tokens.get(0) + "`` doesn't exist.");
            }
        };
        commandSet.add(commandToAdd);


        //.AddResponse
        commandToAdd = new Command();
        commandToAdd.commandName = ".AddResponse";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.tokens.add("");
        commandToAdd.tokens.add("");
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (responses.containsKey(command.tokens.get(0))) {
                    ArrayList<BotMessage> catResponses= responses.get(command.tokens.get(0));
                    MessageScanner messageScanner = new MessageScanner(new StringReader(command.tokens.get(1)));
                    try {
                        BotMessage botMessage = messageScanner.yylex();
                        if (message == null) {
                            message.reply("Something is terribly wrong with your response");
                            return;
                        }
                        if (!botMessage.isValid()) {
                            message.reply("The response contains following errors:" + botMessage.error);
                            return;
                        }

                        botMessage.message = command.tokens.get(1);
                        botMessage.normalizeResources();
                        botMessage.author = message.getAuthor().getId();
                        catResponses.add(botMessage);
                        message.reply("Response successfully added.");
                    } catch (IOException e) {
                        message.reply("Something went wrong, no idea what.");
                    }
                } else
                    message.reply("Category ``" + command.tokens.get(0) + "`` doesn't exist.");
            }
        };
        commandSet.add(commandToAdd);


        //.NotifyMeWarnings
        commandToAdd = new Command();
        commandToAdd.commandName = ".NotifyMeWarnings";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                notifiedMembers.put(message.getAuthor().getMentionTag(), true);
                message.reply("You will be notified of the warnings on your messages.");
                savePermissions();
            }
        };
        commandSet.add(commandToAdd);


        //.Don'tNotifyMeWarnings
        commandToAdd = new Command();
        commandToAdd.commandName = ".Don'tNotifyMeWarnings";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                notifiedMembers.put(message.getAuthor().getMentionTag(), false);
                message.reply("You won't be notified of the warnings on your messages anymore.");
                savePermissions();
            }
        };
        commandSet.add(commandToAdd);


        //.SaveDatabase
        commandToAdd = new Command();
        commandToAdd.commandName = ".SaveDatabase";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.tokens.add("");
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (saveDatabase(command.tokens.get(0)))
                    message.reply("Database successfully saved.");
                else
                    message.reply("There was a problem on saving the database, try again.");
            }
        };
        commandSet.add(commandToAdd);


        //.LoadDatabase
        commandToAdd = new Command();
        commandToAdd.commandName = ".LoadDatabase";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.tokens.add("");
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (loadDatabase(command.tokens.get(0)))
                    message.reply("Database successfully loaded.");
                else
                    message.reply("There was a problem on loading the database, try again.");
            }
        };
        commandSet.add(commandToAdd);


        //.MergeDatabase
        commandToAdd = new Command();
        commandToAdd.commandName = ".MergeDatabase";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.tokens.add("");
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (mergeDatabase(command.tokens.get(0)))
                    message.reply("Database successfully merged.");
                else
                    message.reply("There was a problem on merging the database, try again.");
            }
        };
        commandSet.add(commandToAdd);


        //.Reset
        commandToAdd = new Command();
        commandToAdd.commandName = ".Reset";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                resetDatabase();
                message.reply("Database reset.");
            }
        };
        commandSet.add(commandToAdd);


        //.AddChannelToTest
        commandToAdd = new Command();
        commandToAdd.commandName = ".AddChannelToTest";
        commandToAdd.permission = Command.ADMIN;
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (message.getChannelReceiver() == null) {
                    message.reply("This is not a channel.");
                    return;
                }
                testChannels.add(message.getChannelReceiver().getMentionTag());
                savePermissions();
                message.reply("Channel added to test channels.");
            }
        };
        commandSet.add(commandToAdd);


        //.RemoveChannelFromTest
        commandToAdd = new Command();
        commandToAdd.commandName = ".RemoveChannelFromTest";
        commandToAdd.permission = Command.ADMIN;
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (message.getChannelReceiver() == null) {
                    message.reply("This is not a channel.");
                    return;
                }
                testChannels.remove(message.getChannelReceiver().getMentionTag());
                savePermissions();
                message.reply("Channel removed from test channels.");
            }
        };
        commandSet.add(commandToAdd);

        //.SetMessageAsTest
        commandToAdd = new Command();
        commandToAdd.commandName = ".SetMessageAsTest";
        commandToAdd.permission = Command.ADMIN;
        commandToAdd.tokens.add("");
        commandToAdd.tokens.add("");
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (messages.containsKey(command.tokens.get(0))) {
                    ArrayList<BotMessage> catMessages= messages.get(command.tokens.get(0));
                    int index = Integer.parseInt(command.tokens.get(1));
                    if (catMessages.size() > index && index >= 0) {
                        catMessages.get(index).isTestFeature = true;
                        message.reply("Message #" + command.tokens.get(1) + ": ``" + catMessages.get(index).message + "`` of category: ``" + command.tokens.get(0) + "`` is set as a test feature.");
                    } else if (catMessages.size() == 0)
                        message.reply("Category ``" + command.tokens.get(0) + "`` contains no messages.");
                    else
                        message.reply("Index out of bounds.");
                } else
                    message.reply("Category ``" + command.tokens.get(0) + "`` doesn't exist.");
            }
        };
        commandSet.add(commandToAdd);

        //.SetMessageAsFinal
        commandToAdd = new Command();
        commandToAdd.commandName = ".SetMessageAsFinal";
        commandToAdd.permission = Command.ADMIN;
        commandToAdd.tokens.add("");
        commandToAdd.tokens.add("");
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (messages.containsKey(command.tokens.get(0))) {
                    ArrayList<BotMessage> catMessages= messages.get(command.tokens.get(0));
                    int index = Integer.parseInt(command.tokens.get(1));
                    if (catMessages.size() > index && index >= 0) {
                        catMessages.get(index).isTestFeature = false;
                        message.reply("Message #" + command.tokens.get(1) + ": ``" + catMessages.get(index).message + "`` of category: ``" + command.tokens.get(0) + "`` is set as a final feature and is now available on all non-blacklisted channels.");
                    } else if (catMessages.size() == 0)
                        message.reply("Category ``" + command.tokens.get(0) + "`` contains no messages.");
                    else
                        message.reply("Index out of bounds.");
                } else
                    message.reply("Category ``" + command.tokens.get(0) + "`` doesn't exist.");
            }
        };
        commandSet.add(commandToAdd);

        //.SetResponseAsTest
        commandToAdd = new Command();
        commandToAdd.commandName = ".SetResponseAsTest";
        commandToAdd.permission = Command.ADMIN;
        commandToAdd.tokens.add("");
        commandToAdd.tokens.add("");
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (responses.containsKey(command.tokens.get(0))) {
                    ArrayList<BotMessage> catResponses= responses.get(command.tokens.get(0));
                    int index = Integer.parseInt(command.tokens.get(1));
                    if (catResponses.size() > index && index >= 0) {
                        catResponses.get(index).isTestFeature = true;
                        message.reply("Response #" + command.tokens.get(1) + ": ``" + catResponses.get(index).message + "`` of category: ``" + command.tokens.get(0) + "`` is set as a test feature.");
                    } else if (catResponses.size() == 0)
                        message.reply("Category ``" + command.tokens.get(0) + "`` contains no responses.");
                    else
                        message.reply("Index out of bounds.");
                } else
                    message.reply("Category ``" + command.tokens.get(0) + "`` doesn't exist.");
            }
        };
        commandSet.add(commandToAdd);

        //.SetResponseAsFinal
        commandToAdd = new Command();
        commandToAdd.commandName = ".SetResponseAsFinal";
        commandToAdd.permission = Command.ADMIN;
        commandToAdd.tokens.add("");
        commandToAdd.tokens.add("");
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (responses.containsKey(command.tokens.get(0))) {
                    ArrayList<BotMessage> catResponses= responses.get(command.tokens.get(0));
                    int index = Integer.parseInt(command.tokens.get(1));
                    if (catResponses.size() > index && index >= 0) {
                        catResponses.get(index).isTestFeature = false;
                        message.reply("Response #" + command.tokens.get(1) + ": ``" + catResponses.get(index).message + "`` of category: ``" + command.tokens.get(0) + "`` is set as a final feature and is now available on all non-blacklisted channels.");
                    } else if (catResponses.size() == 0)
                        message.reply("Category ``" + command.tokens.get(0) + "`` contains no responses.");
                    else
                        message.reply("Index out of bounds.");
                } else
                    message.reply("Category ``" + command.tokens.get(0) + "`` doesn't exist.");
            }
        };
        commandSet.add(commandToAdd);

        //.NewProgram
        commandToAdd = new Command();
        commandToAdd.commandName = ".NewProgram";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.tokens.add("");
        commandToAdd.tokens.add("");
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                Pair<String, Boolean> compileResult = JavaSourceCompiler.compileString(command.tokens.get(1), command.tokens.get(0));
                if (compileResult.getValue()) {
                    message.reply("Program successfully added.");
                    programs.put(command.tokens.get(0), command.tokens.get(1));
                } else
                    message.reply("Program has the following errors:" + compileResult);
            }
        };
        commandSet.add(commandToAdd);

        //.RemoveProgram
        commandToAdd = new Command();
        commandToAdd.commandName = ".RemoveProgram";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.tokens.add("");
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (programs.remove(command.tokens.get(0)) != null) {
                    message.reply("Program successfully removed.");
                } else
                    message.reply("Program does not exist.");
            }
        };
        commandSet.add(commandToAdd);

        //.ListPrograms
        commandToAdd = new Command();
        commandToAdd.commandName = ".ListPrograms";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (programs.keySet().size() != 0) {
                    String keys = "List of programs:";
                    for (String key : programs.keySet())
                        keys = keys + "\n" + key;
                    message.reply(keys);
                } else
                    message.reply("No programs exit.");
            }
        };
        commandSet.add(commandToAdd);

        //.SetMessageInterrupting
        commandToAdd = new Command();
        commandToAdd.commandName = ".SetMessageInterrupting";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.tokens.add("");
        commandToAdd.tokens.add("");
        commandToAdd.tokens.add("");
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                if (messages.containsKey(command.tokens.get(0))) {
                    ArrayList<BotMessage> catMessages= messages.get(command.tokens.get(0));
                    int index = Integer.parseInt(command.tokens.get(1));
                    if (catMessages.size() > index && index >= 0) {
                        if (command.tokens.get(2).toLowerCase().equals("true") || command.tokens.get(2).toLowerCase().equals("false")) {
                            catMessages.get(index).isInterrupting = command.tokens.get(2).toLowerCase().equals("true");
                            message.reply("Message #" + command.tokens.get(1) + ": ``" + catMessages.get(index).message + "`` of category: ``" + command.tokens.get(0) + (catMessages.get(index).isInterrupting ? "`` is set as interrupting." : "`` is set as non-interrupting."));
                        } else
                            message.reply("Third token of the command should be true or false.");
                    } else if (catMessages.size() == 0)
                        message.reply("Category ``" + command.tokens.get(0) + "`` contains no messages.");
                    else
                        message.reply("Index out of bounds.");
                } else
                    message.reply("Category ``" + command.tokens.get(0) + "`` doesn't exist.");
            }
        };
        commandSet.add(commandToAdd);

        //?Help
        commandToAdd = new Command();
        commandToAdd.commandName = "?Help";
        commandToAdd.permission = Command.WHITE;
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                message.reply("```" +
                        ".NewCat [Cat] \n" +
                        ".AddMessage [Cat][Message]\n" +
                        ".AddResponse [Cat][Response]\n" +
                        ".ListCats\n" +
                        ".ListMessages [Cat]\n" +
                        ".ListResponses [Cat]\n" +
                        ".RemoveCat [Cat]\n" +
                        ".RemoveMessage [Cat][Message]\n" +
                        ".RemoveResponse [Cat][Response]\n" +
                        ".NewRes [Res]\n" +
                        ".AddToRes [Res][Element]\n" +
                        ".ListRess\n" +
                        ".ListResElements [Res]\n" +
                        ".RemoveRes [Res]\n" +
                        ".RemoveFromRes [Res][Element]\n" +
                        ".NewProgram [Class][Program]\n" +
                        ".RemoveProgram [Class]\n" +
                        ".ListPrograms\n" +
                        ".Permission [Channel/Member/Role] [White/Black/Admin] [Tag]\n" +
                        ".NotifyMeWarnings\n" +
                        ".Don'tNotifyMeWarnings\n" +
                        ".SaveDatabase [File Name]\n" +
                        ".LoadDatabase [File Name]\n" +
                        ".MergerDatabase [File Name]\n" +
                        ".Reset\n" +
                        ".AddChannelToTest\n" +
                        ".RemoveChannelFromTest\n" +
                        ".SetMessageAsTest [Category][Message ID]\n" +
                        ".SetMessageAsFinal[Category][Message ID]\n" +
                        ".SetResponseAsTest [Category][Response ID]\n" +
                        ".SetResponseAsFinal[Category][Response ID]\n" +
                        ".EditMessage[Category][Message ID][Message]\n" +
                        ".EditResponse[Category][Response ID][Response]\n" +
                        ".SetMessageInterrupting [Category][Message ID][True/False]\n" +
                        "?Help\n" +
                        ".FullDoc\n" +
                        ".SourceCode```");
            }
        };
        commandSet.add(commandToAdd);

        //.FullDoc
        commandToAdd = new Command();
        commandToAdd.commandName = ".FullDoc";
        commandToAdd.permission = Command.NONE;
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                message.reply("https://www.gamedev.net/blogs/entry/2263548-iyp-bot-20/");
            }
        };
        commandSet.add(commandToAdd);

        //.SourceCode
        commandToAdd = new Command();
        commandToAdd.commandName = ".SourceCode";
        commandToAdd.permission = Command.NONE;
        commandToAdd.function = new CommandFunction() {
            public void doFunction(Command command, Message message) {
                message.reply("https://github.com/IYP-Programer-Yeah/IYP_Bot-2.0");
            }
        };
        commandSet.add(commandToAdd);
    }

    private void warnAuthor(final String warning, final Future<User> author) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                while (true) {
                    try {
                        if (notifiedMembers.containsKey(author.get().getMentionTag()) && notifiedMembers.get(author.get().getMentionTag()))
                            author.get().sendMessage(warning);
                        break;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private boolean isValidResource(String name) {
        return name.equals("*") || resourcesCS.containsKey(name);
    }

    private String buildResource(ResourceMessagePart resourceMessagePart, int[] endPoints, String content) {
        if (resourceMessagePart.resourceIndex != -1)
            return content.substring(endPoints[resourceMessagePart.resourceIndex], endPoints[resourceMessagePart.resourceIndex + 1]);
        Random rnd = new Random(System.currentTimeMillis());
        String result = "";
        int count = resourceMessagePart.count;
        if (resourceMessagePart.count < 0) {
            count = Math.abs(rnd.nextInt()) % 10;
            if (resourceMessagePart.count == -2)
                count++;
        }
        if (resourceMessagePart.name.equals("*"))
            return "";
        for (int i = 0; i < count; i++)
            result = result + resourcesCS.get(resourceMessagePart.name).getRandomKey();
        return result;
    }

    private void gatherArguments(String[] args, ProgramMessagePart resourceMessagePart, int[] endPoints, String content) {
        for (int j = 0; j < resourceMessagePart.arguments.size(); j++)
            args[j] = buildResource(resourceMessagePart.arguments.get(j), endPoints, content);
    }

    private boolean runMessageProgram(String className, String functionName, String[] inputs, Message message, BotMessage botMessage, String messageCategory) {
        Object[] args = {inputs, message};

        Class funcClass = JavaSourceCompiler.loadedClasses.get(className);
        if (funcClass == null) {
            warnAuthor("The class: ``" + className + "`` used in message: ``" + botMessage.message + "`` of category ``" + messageCategory +"`` does not exist." ,api.getUserById(botMessage.author));
            return false;
        }
        Object ret = null;
        try {
            ret = funcClass.getDeclaredMethod(functionName, String[].class, Message.class).invoke(null, args);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            warnAuthor("There seems to be a problem with the function: ``" + functionName + "`` of class: ``" + className + "`` message: ``" + botMessage.message + "`` of category ``" + messageCategory +"``" ,api.getUserById(botMessage.author));
            return false;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            warnAuthor("The function: ``" + functionName + "`` of class: ``" + className + "`` used in message: ``" + botMessage.message + "`` of category ``" + messageCategory +"`` does not exist." ,api.getUserById(botMessage.author));
        }
        return (Boolean)ret;
    }

    private ArrayList<Pair<String, Pair<BotMessage, int[]>>> categorizeMessage(Message message) {
        String content = message.getContent();
        String senderMentionTag = message.getAuthor().getMentionTag();
        HashMap <String, NFA> resourcesNFACI = new HashMap<>();
        HashMap <String, NFA> resourcesNFACS = new HashMap<>();
        ArrayList<Pair<String, Pair<BotMessage, int[]>>> results = new ArrayList<>();
        for (Map.Entry<String, ArrayList<BotMessage>> entry : messages.entrySet())//go through categories

            nextMessage: for (BotMessage currentCatMessage : entry.getValue()) {//go through messages in the category

                //check if the resources used in the message actually exists
                for (MessagePart messagePart : currentCatMessage.messageParts)
                    if (messagePart.type == MessagePart.MessagePartType.Resource) {
                        if (!isValidResource(messagePart.resource.name)) {
                            warnAuthor("The resource: ``" + messagePart.resource.name + "`` used in message: ``" + currentCatMessage.message + "`` of category``" + entry.getKey() + "`` does not exist.", api.getUserById(currentCatMessage.author));
                            continue nextMessage;
                        }
                    } else if (messagePart.type == MessagePart.MessagePartType.Program) {
                        for (ResourceMessagePart argument : messagePart.program.arguments)
                            if (!isValidResource(argument.name)) {
                                warnAuthor("The resource: ``" + argument.name + "`` used in message: ``" + currentCatMessage.message + "`` of category``" + entry.getKey() + "`` does not exist.", api.getUserById(currentCatMessage.author));
                                continue nextMessage;
                            }
                    }

                //check test message privilege
                if (message.getChannelReceiver() != null && currentCatMessage.isTestFeature && !testChannels.contains(message.getChannelReceiver().getMentionTag()))
                    continue nextMessage;


                int[] endPoints = new int[currentCatMessage.messageParts.size() + 1];
                nextMessagePart: for (int i=0; i<currentCatMessage.messageParts.size(); i++) {
                    boolean backTrack = false;
                    MessagePart currentMessagePart = currentCatMessage.messageParts.get(i);
                    currentMessagePartSwitch: switch (currentMessagePart.type) {
                        case MentionTag: {
                            String finalMentionTag;
                            if (currentMessagePart.rawMessage.equals("@sender"))
                                finalMentionTag = senderMentionTag;
                            else
                                finalMentionTag = botMentionTag;

                            endPoints[i + 1] = botMentionTag.length() + endPoints[i];
                            if (endPoints[i + 1] > content.length())
                                continue nextMessage;
                            if (!content.substring(endPoints[i], endPoints[i + 1]).equals(finalMentionTag) || (i == endPoints.length - 2 && endPoints[i + 1] < content.length())) {
                                backTrack = true;
                                break currentMessagePartSwitch;
                            }

                            break;
                        }
                        case RawMessage: {
                            endPoints[i + 1] = currentMessagePart.rawMessage.length() + endPoints[i];
                            if (endPoints[i + 1] > content.length())
                                continue nextMessage;
                            String contentSubstring = content.substring(endPoints[i], endPoints[i + 1]);
                            if (!(currentCatMessage.caseSensitive?contentSubstring:contentSubstring.toLowerCase()).equals(currentCatMessage.caseSensitive?currentMessagePart.rawMessage:currentMessagePart.rawMessage.toLowerCase()) || (i == endPoints.length - 2 && endPoints[i + 1] < content.length())) {
                                backTrack = true;
                                break currentMessagePartSwitch;
                            }
                            break;
                        }
                        case Resource: {
                            if (endPoints[i + 1] > content.length()) {//this check needs to be done coz on a return back state we this might get violated
                                backTrack = true;
                                break currentMessagePartSwitch;
                            }
                            if (currentMessagePart.resource.count == 0)//this is useless but still we process it
                                if ((endPoints[i + 1] - endPoints[i]) == 0)//this should take no token, if it does, return back
                                    break;
                                else
                                    continue nextMessage;


                            if (currentMessagePart.resource.name.equals("*")) {//this is a separate case, you don't mix it up
                                //needs nothing to be done?
                                break;
                            }

                            Trie currentResourceTrie = (currentMessagePart.resource.isCaseSensitive?resourcesCS.get(currentMessagePart.resource.name):resourcesCI.get(currentMessagePart.resource.name));
                            HashMap <String, NFA> resourcesNFA = (currentMessagePart.resource.isCaseSensitive?resourcesNFACS:resourcesNFACI);

                            if (currentMessagePart.resource.count < 0) {//either a + or * case happening
                                NFA currentResource;
                                if ((resourcesNFA).containsKey(currentMessagePart.resource.name))
                                    currentResource = resourcesNFA.get(currentMessagePart.resource.name);
                                else {
                                    currentResource = new NFA();
                                    resourcesNFA.put(currentMessagePart.resource.name, currentResource);
                                    for (String res : currentResourceTrie.getKeys())
                                        currentResource.put(res);
                                }
                                LinkedList<NFA> dsts = new LinkedList<>();
                                dsts.add(currentResource);
                                for (int j = endPoints[i]; j < endPoints[i + 1]; j++)
                                    dsts = currentResource.process((currentMessagePart.resource.isCaseSensitive?content:content.toLowerCase()).charAt(j), dsts);
                                while (!currentResource.accepts(dsts) || (currentMessagePart.resource.count == -2 && (endPoints[i + 1] - endPoints[i]) == 0)) {
                                    if (endPoints[i + 1] >= content.length() || dsts.size() == 0) {
                                        backTrack = true;
                                        break currentMessagePartSwitch;
                                    }
                                    dsts = currentResource.process((currentMessagePart.resource.isCaseSensitive?content:content.toLowerCase()).charAt(endPoints[i + 1]), dsts);
                                    endPoints[i + 1]++;
                                }
                            } else {
                                Trie dst = currentResourceTrie;
                                for (int j = endPoints[i]; j < endPoints[i + 1]; j++) {
                                    dst = dst.process((currentMessagePart.resource.isCaseSensitive?content:content.toLowerCase()).charAt(j));
                                    if (dst == null)
                                        break;
                                }

                                while (dst == null || !dst.accepts()) {
                                    if (dst == null || endPoints[i + 1] >= content.length()) {
                                        backTrack = true;
                                        break currentMessagePartSwitch;
                                    }
                                    dst = dst.process((currentMessagePart.resource.isCaseSensitive?content:content.toLowerCase()).charAt(endPoints[i + 1]));
                                    endPoints[i + 1]++;
                                }

                            }
                            break;
                        }
                        case Program: {
                            if (endPoints[i + 1] > content.length()) {//this check needs to be done coz on a return back state we this might get violated
                                backTrack = true;
                                break currentMessagePartSwitch;
                            }


                            String[] args = new String[currentMessagePart.program.arguments.size() + 1];

                            gatherArguments(args, currentMessagePart.program, endPoints, content);
                            args[currentMessagePart.program.arguments.size()] = content.substring(endPoints[i], endPoints[i + 1]);
                            while (!runMessageProgram(currentMessagePart.program.className, currentMessagePart.program.functionName,args, message, currentCatMessage, entry.getKey())) {//check if there the program accepts
                                endPoints[i + 1]++;
                                if (endPoints[i + 1] > content.length()) {
                                    backTrack = true;
                                    break currentMessagePartSwitch;
                                }
                                args[currentMessagePart.program.arguments.size()] = content.substring(endPoints[i], endPoints[i + 1]);
                            }

                            break;
                        }
                    }


                    if (backTrack) {//back track and find an other possible combination
                        i = currentCatMessage.findPreviousVariablesSizeMessagePart(i);
                        if (i < 0)
                            continue nextMessage;
                        endPoints[i + 1]++;
                        i--;
                        continue nextMessagePart;
                    }


                    if (i == endPoints.length - 2 && endPoints[i+1] == content.length()) {
                        results.add(new Pair<String, Pair<BotMessage, int[]>>(entry.getKey(), new Pair<BotMessage, int[]>(currentCatMessage, endPoints)));
                        if (currentCatMessage.isInterrupting)
                            return results;
                    } else if (i < endPoints.length - 3)
                        endPoints[i + 2] = endPoints[i + 1];
                    else if (i == endPoints.length - 3)
                        endPoints[i + 2] = content.length();
                }
            }
        return results;
    }

    private String runResponseProgram(String className, String functionName, String[] inputs, Message message, BotMessage response, String messageCategory) {
        Object[] args = {inputs, message};

        Class funcClass = JavaSourceCompiler.loadedClasses.get(className);
        if (funcClass == null) {
            warnAuthor("The class: ``" + className + "`` used in response: ``" + response.message + "`` of category ``" + messageCategory +"`` does not exist." ,api.getUserById(response.author));
            return null;
        }
        Object ret = null;
        try {
            ret = funcClass.getDeclaredMethod(functionName, String[].class, Message.class).invoke(null, args);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            warnAuthor("There seems to be a problem with the function: ``" + functionName + "`` of class: ``" + className + "`` response: ``" + response.message + "`` of category ``" + messageCategory +"``" ,api.getUserById(response.author));
            return null;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            warnAuthor("The function: ``" + functionName + "`` of class: ``" + className + "`` response: ``" + response.message + "`` of category ``" + messageCategory +"`` does not exist." ,api.getUserById(response.author));
        }
        return (String)ret;
    }

    private String generateResponse(Pair<String, Pair<BotMessage, int[]>> category, Message message) {
        String senderMentionTag = message.getAuthor().getMentionTag();
        String content = message.getContent();

        LinkedList<BotMessage> viableResponses = new LinkedList<>();
        LinkedList<BotMessage> perfectResponses = new LinkedList<>();

        BotMessage minimumDanglingResourceResponse = null;
        int minimumDanglingRecourseCount;

        int[] endPoints = category.getValue().getValue();

        ArrayList<BotMessage> catResponses = responses.get(category.getKey());

        if (catResponses.size() == 0) {
            warnAuthor( "No responses for message: ``" + category.getValue().getKey().message + "`` of category``" + category.getKey() + "`` exists.", api.getUserById(category.getValue().getKey().author));
            return null;
        }

        ArrayList<Pair<Integer, ResourceMessagePart>> messageResources = category.getValue().getKey().getAvailableResources();

        minimumDanglingRecourseCount = catResponses.get(0).messageParts.size();

        nextResponse: for (BotMessage response : catResponses) {

            //check if the resources used in the response actually exists
            for (MessagePart messagePart : response.messageParts)
                if (messagePart.type == MessagePart.MessagePartType.Resource) {
                    if (!isValidResource(messagePart.resource.name)) {
                        warnAuthor("The resource: ``" + messagePart.resource.name + "`` used in response: ``" + response.message + "`` of category``" + category.getKey() + "`` does not exist.", api.getUserById(response.author));
                        continue nextResponse;
                    }
                } else if (messagePart.type == MessagePart.MessagePartType.Program) {
                    for (ResourceMessagePart argument : messagePart.program.arguments)
                        if (!isValidResource(argument.name)) {
                            warnAuthor("The resource: ``" + argument.name + "`` used in response: ``" + response.message + "`` of category``" + category.getKey() + "`` does not exist.", api.getUserById(response.author));
                            continue nextResponse;
                        }
                }

            //check test message privilege
            if (message.getChannelReceiver() != null && response.isTestFeature && !testChannels.contains(message.getChannelReceiver().getMentionTag()))
                continue nextResponse;

            Pair<Boolean, Integer> responseConditions = response.evaluateReferencedResourceIndices(messageResources);

            if (responseConditions.getValue() < minimumDanglingRecourseCount) {
                minimumDanglingRecourseCount = responseConditions.getValue();
                minimumDanglingResourceResponse = response;
            }
            if (responseConditions.getValue() == 0) {
                viableResponses.add(response);

                if (responseConditions.getKey())
                    perfectResponses.add(response);
            }
        }
        BotMessage response;

        Random rnd = new Random(System.currentTimeMillis());

        if (perfectResponses.size() != 0) {
            response = perfectResponses.get(Math.abs(rnd.nextInt())%perfectResponses.size());
        } else if (viableResponses.size() != 0) {
            warnAuthor("No perfect response for message: ``" + category.getValue().getKey().message + "`` of category: ``" + category.getKey() + "``", api.getUserById(category.getValue().getKey().author));
            response = viableResponses.get(Math.abs(rnd.nextInt())%viableResponses.size());
        } else {
            warnAuthor("No viable response for message: ``" + category.getValue().getKey().message + "`` of category: ``" + category.getKey() + "``", api.getUserById(category.getValue().getKey().author));
            response = minimumDanglingResourceResponse;
        }

        String generatedResponse = "";

        for (MessagePart currentMessagePart : response.messageParts) {
            switch (currentMessagePart.type) {
                case RawMessage: {
                    generatedResponse = generatedResponse + currentMessagePart.rawMessage;
                    break;
                }
                case MentionTag: {
                    generatedResponse = generatedResponse + (currentMessagePart.rawMessage.equals("@sender")?senderMentionTag:botMentionTag);
                    break;
                }
                case Resource:{
                    generatedResponse = generatedResponse + buildResource(currentMessagePart.resource, endPoints, content);
                    break;
                }
                case Program: {
                    String[] args = new String[currentMessagePart.program.arguments.size()];
                    gatherArguments(args, currentMessagePart.program, endPoints, content);
                    String programResponse = runResponseProgram(currentMessagePart.program.className, currentMessagePart.program.functionName, args, message, response, category.getKey());
                    if (programResponse == null)
                        return null;
                    generatedResponse = generatedResponse + programResponse;
                    break;
                }
            }
        }
        return generatedResponse;
    }

    public Bot(String token, String saveDirectory) {
        this.saveDirectory = saveDirectory;
        api = Javacord.getApi(token, true);
        // connect
        api.connect(new FutureCallback<DiscordAPI>() {
            public void onSuccess(DiscordAPI api) {
                setUpCommandSet();

                botMentionTag = api.getYourself().getMentionTag();

                api.setGame("?help for help");

                loadPermissions();
                // register listener
                api.registerListener(new MessageCreateListener() {
                    public void onMessageCreate(DiscordAPI api, Message message) {
                        if (message.getAuthor().getMentionTag().equals(botMentionTag))
                            return;
                        ArrayList<Command> commands = acquireCommands(message);
                        if (commands.size()!=0) {
                            for (Command command : commands)
                                command.function.doFunction(command, message);
                        } else if (acquirePermission(message) != Command.BLACK) {
                            ArrayList<Pair<String, Pair<BotMessage, int[]>>> messages = categorizeMessage(message);
                            for (Pair<String, Pair<BotMessage, int[]>> categorizedMessage : messages) {
                                String response = generateResponse(categorizedMessage, message);
                                if (response != null)
                                    message.reply(response);
                            }
                        }
                    }
                });
            }
            public void onFailure(Throwable t) {
                t.printStackTrace();
            }

        });
    }
}