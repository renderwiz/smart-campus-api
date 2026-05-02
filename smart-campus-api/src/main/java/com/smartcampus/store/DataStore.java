package com.smartcampus.store;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton in-memory data store.
 *
 * Uses ConcurrentHashMap to handle concurrent requests safely without
 * explicit synchronization on most read operations. Write operations
 * that involve multiple steps are synchronized on the maps themselves.
 *
 * Since JAX-RS creates a new Resource instance per request, ALL shared
 * state MUST live here — not inside resource classes.
 */
public class DataStore {

    // ---- Singleton ----
    private static final DataStore INSTANCE = new DataStore();
    public static DataStore getInstance() { return INSTANCE; }

    // ---- Storage ----
    private final Map<String, Room>          rooms    = new ConcurrentHashMap<>();
    private final Map<String, Sensor>        sensors  = new ConcurrentHashMap<>();
    private final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    private DataStore() {
        seedData();
    }

    /** Pre-populate a few demo records so the API works out-of-the-box. */
    private void seedData() {
        Room r1 = new Room("LIB-301", "Library Quiet Study", 40);
        Room r2 = new Room("LAB-101", "Computer Science Lab", 30);
        rooms.put(r1.getId(), r1);
        rooms.put(r2.getId(), r2);

        Sensor s1 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 22.5, "LIB-301");
        Sensor s2 = new Sensor("CO2-001",  "CO2",         "ACTIVE", 410.0, "LIB-301");
        Sensor s3 = new Sensor("OCC-001",  "Occupancy",   "MAINTENANCE", 15.0, "LAB-101");
        sensors.put(s1.getId(), s1);
        sensors.put(s2.getId(), s2);
        sensors.put(s3.getId(), s3);

        r1.getSensorIds().add("TEMP-001");
        r1.getSensorIds().add("CO2-001");
        r2.getSensorIds().add("OCC-001");

        readings.put("TEMP-001", new ArrayList<>());
        readings.put("CO2-001",  new ArrayList<>());
        readings.put("OCC-001",  new ArrayList<>());
    }

    // ---- Room operations ----
    public Map<String, Room> getRooms()               { return rooms; }
    public Room getRoom(String id)                    { return rooms.get(id); }
    public void putRoom(String id, Room room)         { rooms.put(id, room); }
    public boolean deleteRoom(String id)              { return rooms.remove(id) != null; }

    // ---- Sensor operations ----
    public Map<String, Sensor> getSensors()           { return sensors; }
    public Sensor getSensor(String id)                { return sensors.get(id); }
    public void putSensor(String id, Sensor sensor)   { sensors.put(id, sensor); }
    public boolean deleteSensor(String id)            { return sensors.remove(id) != null; }

    // ---- Reading operations ----
    public List<SensorReading> getReadings(String sensorId) {
        return readings.computeIfAbsent(sensorId, k -> new ArrayList<>());
    }
    public void addReading(String sensorId, SensorReading reading) {
        getReadings(sensorId).add(reading);
    }
}
