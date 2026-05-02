package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.store.DataStore;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Part 2 - Room Management
 * Manages /api/v1/rooms
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    // ----------------------------------------------------------------
    // GET /api/v1/rooms  - list all rooms
    // ----------------------------------------------------------------
    @GET
    public Response getAllRooms() {
        List<Room> roomList = new ArrayList<>(store.getRooms().values());
        return Response.ok(roomList).build();
    }

    // ----------------------------------------------------------------
    // POST /api/v1/rooms  - create a new room
    // ----------------------------------------------------------------
    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getId() == null || room.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(
                        "status",  400,
                        "error",   "Bad Request",
                        "message", "Room id is required."
                    )).build();
        }
        if (store.getRoom(room.getId()) != null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of(
                        "status",  409,
                        "error",   "Conflict",
                        "message", "Room with id '" + room.getId() + "' already exists."
                    )).build();
        }
        store.putRoom(room.getId(), room);
        return Response.status(Response.Status.CREATED).entity(room).build();
    }

    // ----------------------------------------------------------------
    // GET /api/v1/rooms/{roomId}  - get a specific room
    // ----------------------------------------------------------------
    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRoom(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                        "status",  404,
                        "error",   "Not Found",
                        "message", "Room '" + roomId + "' does not exist."
                    )).build();
        }
        return Response.ok(room).build();
    }

    // ----------------------------------------------------------------
    // DELETE /api/v1/rooms/{roomId}
    // Business rule: cannot delete if sensors still assigned.
    // Idempotent: first call removes, subsequent calls return 404 (not 500).
    // ----------------------------------------------------------------
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRoom(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                        "status",  404,
                        "error",   "Not Found",
                        "message", "Room '" + roomId + "' does not exist."
                    )).build();
        }
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId, room.getSensorIds().size());
        }
        store.deleteRoom(roomId);
        return Response.noContent().build();
    }
}
