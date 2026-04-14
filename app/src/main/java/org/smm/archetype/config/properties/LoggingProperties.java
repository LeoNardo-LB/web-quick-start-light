package org.smm.archetype.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "logging")
public class LoggingProperties {

    private SlowQuery slowQuery = new SlowQuery();
    private Sampling  sampling  = new Sampling();

    @Getter
    @Setter
    public static class SlowQuery {

        private long    thresholdMs = 1000L;
        private boolean enabled     = false;

    }

    @Getter
    @Setter
    public static class Sampling {

        private boolean enabled = false;
        private double  rate    = 0.1;

    }

}
