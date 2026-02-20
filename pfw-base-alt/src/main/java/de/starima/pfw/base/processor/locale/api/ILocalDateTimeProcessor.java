package de.starima.pfw.base.processor.locale.api;

import de.starima.pfw.base.processor.api.IProcessor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public interface ILocalDateTimeProcessor extends IProcessor {
    public LocalDateTime toLocalDateTime(Instant instant);
    public LocalDate toLocalDate(Instant instant);
    public LocalDateTime toLocalDateTime(Date date);
    public LocalDate toLocalDate(Date date);
    public long daysBetween(LocalDateTime min, LocalDateTime max);
    public long daysBetween(LocalDate min, LocalDate max);
    public Date toDate(LocalDateTime localDateTime);
    public Date toDate(LocalDate localDate);
    public LocalDateTime toLocalDateTime(long milliseconds);
    public LocalDate toLocalDate(long milliseconds);
    public LocalDateTime atStartOfDay(Date date);
    public boolean isInInterval(LocalDateTime toCheck, LocalDateTime intervalStart, LocalDateTime intervalEnd);
    public ZoneId getZoneId();
    public void setZoneId(String zoneId);
}