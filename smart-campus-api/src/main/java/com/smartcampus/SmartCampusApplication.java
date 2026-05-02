package com.smartcampus;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS Application configuration.
 * Sets the base URI path to /api/v1 for all resources.
 *
 * Lifecycle note: By default, JAX-RS creates a NEW instance of each
 * Resource class per request (request-scoped). This means we cannot
 * store state inside resource fields. We use a shared static DataStore
 * (singleton) with ConcurrentHashMap to safely handle concurrent requests.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {
    // Jersey auto-scans packages; no manual registration needed.
}
