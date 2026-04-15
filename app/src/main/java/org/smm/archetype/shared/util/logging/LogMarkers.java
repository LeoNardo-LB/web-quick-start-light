package org.smm.archetype.shared.util.logging;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public final class LogMarkers {

    public static final Marker USER     = MarkerFactory.getMarker("USER");
    public static final Marker SECURITY = MarkerFactory.getMarker("SECURITY");
    public static final Marker AUDIT    = MarkerFactory.getMarker("AUDIT");
    public static final Marker SYSTEM   = MarkerFactory.getMarker("SYSTEM");

    private LogMarkers() {}

}
