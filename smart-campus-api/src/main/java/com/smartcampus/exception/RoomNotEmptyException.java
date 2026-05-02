package com.smartcampus.exception;

/**
 * Thrown when a room deletion is attempted but the room still has sensors.
 * Mapped to HTTP 409 Conflict.
 */
public class RoomNotEmptyException extends RuntimeException {
    private final String roomId;
    private final int    sensorCount;

    public RoomNotEmptyException(String roomId, int sensorCount) {
        super("Room '" + roomId + "' cannot be deleted: it has " + sensorCount + " sensor(s) still assigned.");
        this.roomId      = roomId;
        this.sensorCount = sensorCount;
    }

    public String getRoomId()    { return roomId; }
    public int    getSensorCount() { return sensorCount; }
}
