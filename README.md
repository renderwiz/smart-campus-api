# Smart Campus Sensor & Room Management API

A JAX-RS RESTful API built with Jersey 3 and embedded Grizzly server for the University of Westminster "Smart Campus" initiative.

---

## API Overview

The API manages two primary resources — **Rooms** and **Sensors** — with full CRUD, sub-resource reading history, advanced error handling, and request/response logging.

| Base URL | `http://localhost:8080/api/v1` |
|----------|-------------------------------|
| Format   | JSON only                      |
| Auth     | None (out of scope)            |

### Resource Hierarchy

```
/api/v1
├── /rooms
│   ├── GET     - list all rooms
│   ├── POST    - create a room
│   └── /{id}
│       ├── GET    - get room details
│       └── DELETE - delete room (blocked if sensors present)
└── /sensors
    ├── GET     - list sensors (optional ?type= filter)
    ├── POST    - register sensor (validates roomId)
    └── /{id}
        ├── GET    - get sensor details
        ├── DELETE - remove sensor
        └── /readings
            ├── GET  - list all readings for sensor
            └── POST - add new reading (blocked if MAINTENANCE)
```

---

## How to Build & Run

### Prerequisites

- **Java 11+** (tested with Java 21)
- **Maven 3.6+**
- Internet connection (to download dependencies on first build)

### Step 1 — Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/smart-campus-api.git
cd smart-campus-api
```

### Step 2 — Build

```bash
mvn clean package
```

This produces `target/smart-campus-api-1.0.0.jar` (a fat/uber JAR containing all dependencies).

### Step 3 — Run

```bash
java -jar target/smart-campus-api-1.0.0.jar
```

The server starts on **http://localhost:8080/api/v1**. Press `ENTER` to stop.

---

## Sample curl Commands

### 1. Discovery Endpoint
```bash
curl -X GET http://localhost:8080/api/v1
```

### 2. List All Rooms
```bash
curl -X GET http://localhost:8080/api/v1/rooms
```

### 3. Create a New Room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"HALL-205","name":"Lecture Hall 205","capacity":120}'
```

### 4. Get a Specific Room
```bash
curl -X GET http://localhost:8080/api/v1/rooms/LIB-301
```

### 5. Attempt to Delete a Room with Sensors (should return 409)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

### 6. Register a New Sensor (validates roomId)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-002","type":"Temperature","status":"ACTIVE","currentValue":21.0,"roomId":"HALL-205"}'
```

### 7. Filter Sensors by Type
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2"
```

### 8. Add a Sensor Reading
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":23.7}'
```

### 9. Get Reading History
```bash
curl -X GET http://localhost:8080/api/v1/sensors/TEMP-001/readings
```

### 10. Try to Add Reading to Maintenance Sensor (should return 403)
```bash
curl -X POST http://localhost:8080/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":5.0}'
```

### 11. Register Sensor with Invalid roomId (should return 422)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"BAD-001","type":"Temperature","status":"ACTIVE","currentValue":0.0,"roomId":"DOES-NOT-EXIST"}'
```

---

## Conceptual Report (Q&A)

### Part 1.1 — JAX-RS Resource Lifecycle

By default JAX-RS creates a **new instance of a Resource class for every incoming HTTP request** (per-request scope). This means instance fields on resource classes are not shared across requests and cannot be used for persistent state. To safely manage shared in-memory data structures we use a **singleton `DataStore`** backed by `ConcurrentHashMap`. This map provides thread-safe read operations and atomic put/remove operations without external locks. For compound operations (e.g. checking room existence then adding a sensor) we synchronize on the relevant map object or rely on the fact that HashMap operations are atomic at the individual call level for our use case.

### Part 1.2 — HATEOAS

HATEOAS (Hypermedia As The Engine Of Application State) means embedding navigation links inside API responses rather than requiring clients to construct URLs manually. The discovery endpoint returns `"links"` with `rel`, `method`, and `href` fields. This benefits client developers because: (1) clients are decoupled from hard-coded URL patterns; (2) the server can change URIs without breaking clients that follow links; (3) clients can discover available actions dynamically, reducing the reliance on external documentation.

### Part 2.1 — IDs vs Full Objects in List Responses

Returning **only IDs** is bandwidth-efficient when clients need minimal metadata, but forces N additional round-trips to fetch details. Returning **full objects** increases payload size but allows clients to render all data in a single request. The right choice depends on use case: list views typically need full objects (our implementation), while embedded references (e.g. `sensorIds` inside a Room) use IDs to avoid circular JSON and to keep each resource self-contained.

### Part 2.2 — DELETE Idempotency

Our DELETE implementation **is idempotent** as required by REST semantics. The first `DELETE /rooms/HALL-205` succeeds with `204 No Content`. Subsequent identical requests return `404 Not Found` because the room no longer exists. Importantly, this is still idempotent: the server-side *state* after any number of DELETE calls is identical — the room does not exist. The different HTTP status codes (204 vs 404) are acceptable because idempotency concerns state, not response codes.

### Part 3.1 — @Consumes and Content-Type Mismatch

The `@Consumes(MediaType.APPLICATION_JSON)` annotation tells JAX-RS that a method only accepts requests with `Content-Type: application/json`. If a client sends `text/plain` or `application/xml`, JAX-RS returns **415 Unsupported Media Type** automatically before the method is ever invoked. The request body is not deserialized and the method body does not execute. This is part of JAX-RS's content negotiation layer and requires no code from the developer.

### Part 3.2 — @QueryParam vs Path Segment for Filtering

Query parameters (`?type=CO2`) are **superior for filtering** because: (1) the collection URI `/sensors` remains stable regardless of filter values; (2) parameters are optional by nature, so no filter returns all items; (3) multiple filters combine naturally (`?type=CO2&status=ACTIVE`); (4) it is semantically correct — `type` is a search criterion, not a resource identifier. Path segments like `/sensors/type/CO2` imply a distinct resource hierarchy, are harder to combine, and make the "no filter" case ambiguous.

### Part 4.1 — Sub-Resource Locator Benefits

The sub-resource locator pattern delegates to a **dedicated class** (`SensorReadingResource`) rather than defining every nested path in one controller. Benefits: (1) **Separation of concerns** — reading logic is isolated from sensor management; (2) **Testability** — each class can be unit-tested independently; (3) **Maintainability** — a single 500-line resource class becomes impossible to navigate; (4) **Reusability** — `SensorReadingResource` could theoretically be reused in other contexts. JAX-RS resolves the method at runtime by calling the locator method to obtain the sub-resource instance, then dispatching the matched method on that instance.

### Part 5.2 — 422 vs 404 for Missing Reference

`404 Not Found` means the *requested URL itself* does not exist. In our case the URL `/sensors` is valid — the request is syntactically correct. The *payload* references a `roomId` that does not exist in the system. `422 Unprocessable Entity` communicates that the **request was understood** but **semantically invalid** — the server received a valid JSON body but could not process it because a dependency (`roomId`) was unresolvable. This gives API consumers a more actionable error signal than a generic 404.

### Part 5.4 — Stack Trace Security Risk

Exposing Java stack traces leaks: (1) **Internal package names and class names** — attackers learn the application's structure; (2) **Framework versions** — known CVEs can be targeted; (3) **File paths** — may reveal server OS layout; (4) **Business logic flows** — method call sequences expose implementation details useful for crafting targeted exploits. Our `GlobalExceptionMapper` logs the full trace **server-side** for debugging but returns only a generic message to the client, following the principle of least privilege.

### Part 5.5 — Filters vs Inline Logging

Using a JAX-RS filter for logging is superior because: (1) **DRY principle** — logging code exists in exactly one place; (2) **Consistency** — every endpoint is guaranteed to be logged, including endpoints added in the future; (3) **Separation of concerns** — resource methods remain focused on business logic; (4) **Configurability** — the filter can be toggled or replaced (e.g. swap to structured JSON logging) without touching resource classes.

---

## Project Structure

```
smart-campus-api/
├── pom.xml
└── src/main/java/com/smartcampus/
    ├── Main.java                          # Embedded Grizzly server entry point
    ├── SmartCampusApplication.java        # @ApplicationPath("/api/v1")
    ├── model/
    │   ├── Room.java
    │   ├── Sensor.java
    │   └── SensorReading.java
    ├── store/
    │   └── DataStore.java                 # Singleton ConcurrentHashMap store
    ├── resource/
    │   ├── DiscoveryResource.java         # GET /api/v1
    │   ├── RoomResource.java              # /api/v1/rooms
    │   ├── SensorResource.java            # /api/v1/sensors
    │   └── SensorReadingResource.java     # /api/v1/sensors/{id}/readings
    ├── exception/
    │   ├── RoomNotEmptyException.java
    │   ├── LinkedResourceNotFoundException.java
    │   ├── SensorUnavailableException.java
    │   └── ExceptionMappers.java          # All 4 @Provider mappers
    └── filter/
        └── LoggingFilter.java             # Request + Response logging
```

---

## Technology Stack

- **Java 11** (source/target compatibility)
- **JAX-RS 3.1** (Jakarta EE)
- **Jersey 3.1.3** (JAX-RS implementation)
- **Grizzly 2** (embedded HTTP server, no external servlet container needed)
- **Jackson** (JSON serialisation via jersey-media-json-jackson)
- **Maven** (build tool, shade plugin for fat JAR)
- **In-memory storage** (ConcurrentHashMap — no database)
