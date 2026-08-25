package com.terminal_devilal.business_tools.pvpp.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@IdClass(PvppGenerationHistoryId.class)
@Table(name = "pvpp_generation_history")
public class PvppGenerationHistoryEntity {

    @Id
    @Column(name = "date")
    private LocalDate date;

    @Id
    @Column(name = "days")
    private Integer days;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PvppGenerationStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "skipped_tickers")
    private String skippedTickers;

    public PvppGenerationHistoryEntity() {
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public PvppGenerationStatus getStatus() {
        return status;
    }

    public void setStatus(PvppGenerationStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getSkippedTickers() {
        return skippedTickers;
    }

    public void setSkippedTickers(String skippedTickers) {
        this.skippedTickers = skippedTickers;
    }
}
