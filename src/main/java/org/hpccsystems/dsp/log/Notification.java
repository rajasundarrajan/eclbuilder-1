package org.hpccsystems.dsp.log;

public class Notification extends DBLog {

    private static final long serialVersionUID = 1L;
    public static final String APPROVER_NOTIFICATION_FAILED = "Approver notification failed";
    public static final String STATIC_FILE_MIGRATION_FAILED = "StaticFile/Dermotology migration failed";
    public static final String INVALID_COMPOSITION = "Invalid composition";

    private String message;

    public Notification(String sessionId, String userId, long startTime, String action, String message) {
        super(sessionId, userId, startTime, action);
        this.message = message;
    }

    @Override
    public String getDetail() {
        return message;
    }

}
