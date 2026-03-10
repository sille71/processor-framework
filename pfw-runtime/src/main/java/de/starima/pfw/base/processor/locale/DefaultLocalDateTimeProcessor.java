package de.starima.pfw.base.processor.locale;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.locale.api.ILocalDateTimeProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Getter
@Setter
@Slf4j
@Processor
public class DefaultLocalDateTimeProcessor extends AbstractProcessor implements ILocalDateTimeProcessor {
    @ProcessorParameter(description = "Timezone which is used in the respective recon to operate with LocalDate and LocalDateTime objects.\n" +
            " Default is \"Europe/Berlin\" (see https://code2care.org/pages/java-timezone-list-utc-gmt-offset for more examples)", value = "Europe/Berlin")
    private String zoneId = "Europe/Berlin";

    public LocalDateTime toLocalDateTime(Instant instant) {
        return instant.atZone(ZoneId.of(zoneId)).toLocalDateTime();
    }

    public LocalDate toLocalDate(Instant instant) {
        return instant.atZone(ZoneId.of(zoneId)).toLocalDate();
    }

    public LocalDateTime toLocalDateTime(Date date) {
        if (date == null)
            return toLocalDateTime(Instant.now());
        else
            return toLocalDateTime(date.toInstant());
    }

    public LocalDate toLocalDate(Date date) {
        if (date == null)
            return toLocalDate(Instant.now());
        else
            return toLocalDate(date.toInstant());
    }

    public long daysBetween(LocalDateTime min, LocalDateTime max) {
        return Duration.between(min, max).toDays();
    }

    public long daysBetween(LocalDate min, LocalDate max) {
        return ChronoUnit.DAYS.between(min, max);
    }

    public Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.of(zoneId)).toInstant());
    }

    public Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.of(zoneId)).toInstant());
    }

    public LocalDateTime toLocalDateTime(long milliseconds) {
        return Instant.ofEpochMilli(milliseconds).atZone(ZoneId.of(zoneId)).toLocalDateTime();
    }

    public LocalDate toLocalDate(long milliseconds) {
        return Instant.ofEpochMilli(milliseconds).atZone(ZoneId.of(zoneId)).toLocalDate();
    }

    public ZoneId getZoneId() {
        return ZoneId.of(zoneId);
    }

    public LocalDateTime atStartOfDay(Date date) {
        if (date == null)
            return toLocalDate(Instant.now()).atStartOfDay();
        else
            return toLocalDate(date.toInstant()).atStartOfDay();
    }

    public boolean isInInterval(LocalDateTime toCheck, LocalDateTime intervalStart, LocalDateTime intervalEnd) {
        if ((toCheck.isAfter(intervalStart) || toCheck.equals(intervalStart)) && (toCheck.isBefore(intervalEnd) || toCheck.equals(intervalEnd)))
            return true;
        else
            return false;
    }
}