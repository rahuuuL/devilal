package com.terminal_devilal.indicators.pdv.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cache.pdv")
public class PDVCacheProperties {

    private boolean enabled = true;
    private double preloadYears = 1.5d;
    private boolean chronicleEnabled = true;
    private int snapshotIntervalMinutes = 300;
    private String directory = "data/cache";
    private String snapshotFileName = "pdv-cache.dat";
    private long chronicleEntries = 2_000_000L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double getPreloadYears() {
        return preloadYears;
    }

    public void setPreloadYears(double preloadYears) {
        this.preloadYears = preloadYears;
    }

    public boolean isChronicleEnabled() {
        return chronicleEnabled;
    }

    public void setChronicleEnabled(boolean chronicleEnabled) {
        this.chronicleEnabled = chronicleEnabled;
    }

    public int getSnapshotIntervalMinutes() {
        return snapshotIntervalMinutes;
    }

    public void setSnapshotIntervalMinutes(int snapshotIntervalMinutes) {
        this.snapshotIntervalMinutes = snapshotIntervalMinutes;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getSnapshotFileName() {
        return snapshotFileName;
    }

    public void setSnapshotFileName(String snapshotFileName) {
        this.snapshotFileName = snapshotFileName;
    }

    public long getChronicleEntries() {
        return chronicleEntries;
    }

    public void setChronicleEntries(long chronicleEntries) {
        this.chronicleEntries = chronicleEntries;
    }
}
