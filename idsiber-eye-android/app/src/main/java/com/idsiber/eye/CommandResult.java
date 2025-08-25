package com.idsiber.eye;

/**
 * Class untuk hasil eksekusi command
 */
public class CommandResult {
    private boolean success;
    private String message;
    private String data;
    
    public CommandResult(boolean success, String message, String data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getData() {
        return data;
    }
    
    @Override
    public String toString() {
        return "CommandResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", data='" + data + '\'' +
                '}';
    }
}