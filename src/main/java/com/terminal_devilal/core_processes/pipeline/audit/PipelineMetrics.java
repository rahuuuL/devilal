package com.terminal_devilal.core_processes.pipeline.audit;

import java.util.concurrent.atomic.AtomicInteger;

public class PipelineMetrics {
    private final AtomicInteger apiReceived = new AtomicInteger();
    private final AtomicInteger parsed = new AtomicInteger();
    private final AtomicInteger pdvtSaved = new AtomicInteger();
    private final AtomicInteger kafkaPublished = new AtomicInteger();
    private final AtomicInteger kafkaConsumed = new AtomicInteger();
    private final AtomicInteger atrProcessed = new AtomicInteger();
    private final AtomicInteger atrSaved = new AtomicInteger();
    private final AtomicInteger rsiProcessed = new AtomicInteger();
    private final AtomicInteger rsiSaved = new AtomicInteger();
    private final AtomicInteger vwapProcessed = new AtomicInteger();
    private final AtomicInteger vwapSaved = new AtomicInteger();

    public void incrementApiReceived() { apiReceived.incrementAndGet(); }
    public void incrementParsed() { parsed.incrementAndGet(); }
    public void incrementPdvtSaved() { pdvtSaved.incrementAndGet(); }
    public void incrementKafkaPublished() { kafkaPublished.incrementAndGet(); }
    public void incrementKafkaConsumed() { kafkaConsumed.incrementAndGet(); }
    public void incrementAtrProcessed() { atrProcessed.incrementAndGet(); }
    public void incrementAtrSaved() { atrSaved.incrementAndGet(); }
    public void incrementRsiProcessed() { rsiProcessed.incrementAndGet(); }
    public void incrementRsiSaved() { rsiSaved.incrementAndGet(); }
    public void incrementVwapProcessed() { vwapProcessed.incrementAndGet(); }
    public void incrementVwapSaved() { vwapSaved.incrementAndGet(); }

    public int getApiReceived() { return apiReceived.get(); }
    public int getParsed() { return parsed.get(); }
    public int getPdvtSaved() { return pdvtSaved.get(); }
    public int getKafkaPublished() { return kafkaPublished.get(); }
    public int getKafkaConsumed() { return kafkaConsumed.get(); }
    public int getAtrProcessed() { return atrProcessed.get(); }
    public int getAtrSaved() { return atrSaved.get(); }
    public int getRsiProcessed() { return rsiProcessed.get(); }
    public int getRsiSaved() { return rsiSaved.get(); }
    public int getVwapProcessed() { return vwapProcessed.get(); }
    public int getVwapSaved() { return vwapSaved.get(); }
}
