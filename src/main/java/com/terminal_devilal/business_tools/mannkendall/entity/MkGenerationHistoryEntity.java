package com.terminal_devilal.business_tools.mannkendall.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@IdClass(MkGenerationHistoryId.class)
@Table(name = "mk_generation_history")
public class MkGenerationHistoryEntity {

    @Id
    @Column(name = "date")
    private LocalDate date;

    @Id
    @Column(name = "days")
    private Integer days;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private MkGenerationStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    public MkGenerationHistoryEntity() {
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

    public MkGenerationStatus getStatus() {
        return status;
    }

    public void setStatus(MkGenerationStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
