package org.smm.archetype.client.log.logging;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public final class LogMarkers {
    public static final Marker ORDER = MarkerFactory.getMarker("ORDER");
    public static final Marker PAYMENT = MarkerFactory.getMarker("PAYMENT");
    public static final Marker USER = MarkerFactory.getMarker("USER");
    public static final Marker SECURITY = MarkerFactory.getMarker("SECURITY");
    public static final Marker AUDIT = MarkerFactory.getMarker("AUDIT");

    private LogMarkers() {}
}
