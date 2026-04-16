package com.graphenelab.communication.crypto;

import java.util.Map;
import java.util.HashMap;

public enum Command {
    SetClient(0),
    Authentication(1),
    Pair(2),
    GetPushNotifications(3),
    Error(4),
    GetDir(5),
    GetFile(6),
    Share(7),
    SetFile(8),
    Delete(9),
    Rename(10),
    Move(11),
    Copy(12),
    CreateDir(13),
    Search(14),
    GetGroup(15),
    AddToGroup(16),
    RemoveFromGroup(17),
    GetStorageInfo(18),
    GetOccupiedSpace(19),
    GetEncryptedQR(20);

    private final int id;
    private static final Map<Integer, Command> map = new HashMap<>();

    static {
        for (Command command : Command.values()) {
            map.put(command.id, command);
        }
    }

    Command(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static Command fromId(int id) {
        return map.get(id);
    }

    // Usage example equivalent function
    public static String getCommandName(int commandId) {
        Command command = fromId(commandId);
        return command != null ? command.name() : "UnknownCommand";
    }
}
