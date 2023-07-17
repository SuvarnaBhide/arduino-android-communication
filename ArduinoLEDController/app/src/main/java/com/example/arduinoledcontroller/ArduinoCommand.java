package com.example.arduinoledcontroller;

import com.google.firebase.firestore.Exclude;

public class ArduinoCommand {

    String documentId;
    private String command;
    private String commandDescription;

    ArduinoCommand(){
        //public no-arg constructor needed
    }


    @Exclude
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public ArduinoCommand(String command, String commandDescription){
        this.command = command;
        this.commandDescription = commandDescription;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getCommandDescription() {
        return commandDescription;
    }

    public void setCommandDescription(String commandDescription) {
        this.commandDescription = commandDescription;
    }
}
