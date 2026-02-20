package de.starima.pfw.base.util;

import de.dzbank.components.utils.StringComparator;
import de.dzbank.components.utils.log.LogOutputHelper;
import de.starima.pfw.base.processor.condition.api.IConditionProcessor;
import de.starima.pfw.base.processor.formatter.AttributeNumberFormatter;
import de.starima.pfw.base.processor.formatter.api.IFormatterProcessor;
import de.starima.pfw.base.processor.formatter.api.IFormatterProviderProcessor;
import de.starima.pfw.base.processor.locale.api.ILocalDateTimeProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
public class CalculationHelper {
    public static boolean equalsBetrag(Number v1, Number v2, int factor) {
        if (v1 == null) {
            return v2 == null;
        }

        if (v2 == null) {
            return false;
        }

        if (v2 instanceof BigDecimal) {
            v2 = ((BigDecimal) v2).multiply(new BigDecimal(factor));
        }

        if (v2 instanceof Double) {
            v2 = ((Double) v2) * factor;

        }

        if (v2 instanceof Float) {
            v2 = ((Float) v2) * factor;

        }

        return v1.equals(v2);
    }

    public static Double getInPercent(Number part, Number all) {
        if (part != null && part.doubleValue() != 0 && all != null
                && all.doubleValue() != 0) {
            return part.doubleValue() / all.doubleValue() * 100;
        } else if ((part == null || part.doubleValue() == 0)
                && (all == null || all.doubleValue() == 0)) {
            return 100.00;
        } else {
            return 0.0;
        }
    }

    public static boolean containsStringArray(List<String[]> matrix,
                                              String[] vec) {
        Assert.notNull(matrix, "Argument matrix must be defined!");
        Assert.notNull(vec, "Argument vec must be defined!");
        boolean match = true;

        for (String[] row : matrix) {
            match = true;

            Assert.isTrue(
                    vec.length == row.length,
                    "Arguments have different length for matrix = "
                            + LogOutputHelper.getModelAsStringBuffer(matrix,
                            "row") + " and vec = "
                            + LogOutputHelper.getModelAsStringBuffer(vec, " "));
            for (int i = 0; i < vec.length; i++) {
                match = match
                        && (StringComparator.compareStrings(vec[i], row[i]) == 0);
            }

            if (match)
                break;
        }
        return match;
    }

    public static boolean matchesStringArray(List<Object[]> matrix,
                                             String[] vec) {
        Assert.notNull(matrix, "Argument matrix must be defined!");
        Assert.notNull(vec, "Argument vec must be defined!");
        boolean match = true;
        Pattern pattern;

        for (Object[] row : matrix) {
            match = true;

            Assert.isTrue(
                    vec.length == row.length,
                    "Arguments have different length for matrix = "
                            + LogOutputHelper.getModelAsStringBuffer(matrix,
                            "row") + " and vec = "
                            + LogOutputHelper.getModelAsStringBuffer(vec, " "));
            for (int i = 0; i < vec.length; i++) {
                if (row[i] == null) {
                    match = match
                            && (StringComparator.compareStrings(vec[i], null) == 0);
                } else if (row[i] instanceof Pattern) {
                    //row[i] is a pattern
                    pattern = (Pattern)row[i];
                    match = match && pattern.matcher(vec[i]).matches();
                } else {
                    match = match
                            && (StringComparator.compareStrings(vec[i], row[i].toString()) == 0);
                }
            }

            if (match)
                break;
        }
        return match;
    }

    public static long binaryToDecimal(String binary) {
        long decimal = 0;
        char[] tokens = binary.toCharArray();
        double pow = 0;
        for (int i = (tokens.length - 1); i >= 0; i--) {
            decimal = decimal
                    + (Long.parseLong(String.valueOf(tokens[i])) * (long) Math
                    .pow(2, pow));
            pow++;
        }
        return decimal;
    }

    public static long booleanArrayToDecimal(Boolean[] arr) {
        long decimal = 0;
        double pow = 0;
        if (arr == null) return 0;

        for (Boolean b : arr) {
            if (Boolean.TRUE.equals(b)) {
                decimal = decimal + ((long) Math.pow(2, pow));
            }
            pow++;
        }
        return decimal;
    }

    public static int getDwellWithoutWeekend(Date startDate, Date endDate, ILocalDateTimeProcessor localDateTimeProcessor) {
        if (startDate == null || endDate == null) return 0;

        if (endDate.before(startDate)) return 0;

        LocalDateTime start = localDateTimeProcessor.atStartOfDay(startDate);
        LocalDateTime end = localDateTimeProcessor.toLocalDateTime(endDate);
        int dwell = Math.toIntExact(localDateTimeProcessor.daysBetween(start, end));

        if (dwell == 0) return 0;

        //count the number of weekend days
        LocalDateTime varDay = start;

        while (localDateTimeProcessor.isInInterval(varDay, start, end) && dwell > 0) {
            if (varDay.getDayOfWeek() == DayOfWeek.SATURDAY || varDay.getDayOfWeek() == DayOfWeek.SUNDAY)
                --dwell;
            varDay = varDay.plusDays(1);
        }

        return dwell;
    }

    public static Number addNumbers(Number a, Number b) {
        a = nullToZero(a);
        b = nullToZero(b);

        if(a instanceof Double || b instanceof Double) {
            return a.doubleValue() + b.doubleValue();
        } else if(a instanceof Float || b instanceof Float) {
            return a.floatValue() + b.floatValue();
        } else if(a instanceof Long || b instanceof Long) {
            return a.longValue() + b.longValue();
        } else if(a instanceof BigDecimal || b instanceof BigDecimal) {
            return a.doubleValue() + b.doubleValue();
        } else {
            return a.intValue() + b.intValue();
        }
    }

    public static Number subtractNumbers(Number a, Number b) {
        a = nullToZero(a);
        b = nullToZero(b);

        if(a instanceof Double || b instanceof Double) {
            return a.doubleValue() - b.doubleValue();
        } else if(a instanceof Float || b instanceof Float) {
            return a.floatValue() - b.floatValue();
        } else if(a instanceof Long || b instanceof Long) {
            return a.longValue() - b.longValue();
        } else if(a instanceof BigDecimal || b instanceof BigDecimal) {
            return a.doubleValue() - b.doubleValue();
        } else {
            return a.intValue() - b.intValue();
        }
    }

    public static BigDecimal toBigDecimal(Object n) {
        if (n == null) return  BigDecimal.ZERO;

        if (n instanceof BigDecimal)
            return (BigDecimal) n;
        else if (n instanceof Double)
            return BigDecimal.valueOf(((Double)n).doubleValue());
        else if (n instanceof Float)
            return BigDecimal.valueOf(((Float)n).floatValue());
        else if (n instanceof Number)
            return BigDecimal.valueOf(((Number)n).doubleValue());
        else if (n instanceof Date)
            return BigDecimal.valueOf(((Date)n).getTime());
        else if (n instanceof String) {
            try {
                AttributeNumberFormatter numberFormatter = new AttributeNumberFormatter();
                numberFormatter.setPattern("#,##0.###");
                return toBigDecimal(numberFormatter.parse((String) n, Locale.ENGLISH));
            } catch (Exception e) {
                log.error("Can not parse {} to BigDecimal", n, e);
            }
        }

        return BigDecimal.ZERO;
    }

    public static Date toDate(Object d) {
        if (d == null) return null;
        if (d instanceof Date)
            return (Date)d;
        else if (d instanceof Long)
            return new Date((Long)d);
        else if (d instanceof oracle.sql.TIMESTAMP) {
            try {
                return new Date(((oracle.sql.TIMESTAMP)d).dateValue().getTime());
            } catch (SQLException e) {
                log.error("{} is not an oracle timestamp", d);
            }
        }
        return null;
    }

    public static Number absDifferenceNumbers(Number a, Number b) {
        a = nullToZero(a);
        b = nullToZero(b);
        if(a instanceof Double || b instanceof Double) {
            return Math.abs(Math.abs(a.doubleValue()) - Math.abs(b.doubleValue()));
        } else if(a instanceof Float || b instanceof Float) {
            return Math.abs(Math.abs(a.floatValue()) - Math.abs(b.floatValue()));
        } else if(a instanceof Long || b instanceof Long) {
            return Math.abs(Math.abs(a.longValue()) - Math.abs(b.longValue()));
        }  else if(a instanceof BigDecimal || b instanceof BigDecimal) {
            return Math.abs(Math.abs(a.doubleValue()) - Math.abs(b.doubleValue()));
        } else {
            return Math.abs(Math.abs(a.intValue()) - Math.abs(b.intValue()));
        }
    }

    public static Number absNumber(Number a) {
        a = nullToZero(a);
        if(a instanceof Double) {
            return Math.abs(Math.abs(a.doubleValue()));
        } else if(a instanceof Float) {
            return Math.abs(Math.abs(a.floatValue()));
        } else if(a instanceof Long) {
            return Math.abs(Math.abs(a.longValue()));
        } else if(a instanceof BigDecimal) {
            return Math.abs(Math.abs(a.doubleValue()));
        } else {
            return Math.abs(Math.abs(a.intValue()));
        }
    }

    public static Number nullToZero(Number n) {
        return (n == null) ?  BigDecimal.ZERO : n;
    }

    public static Date getDateFromValue(Map<String, Object> row, String attrName) throws Exception {
        Object oValue = row.get(attrName);
        if (oValue != null) {
            if (oValue instanceof Date) {
                return ((Date) oValue);
            } else if (oValue instanceof oracle.sql.TIMESTAMP) {
                try {
                    return new Date(((oracle.sql.TIMESTAMP) oValue).dateValue().getTime());
                } catch (SQLException e) {
                    throw new Exception(String.format("%s ist kein Oracle Timestamp fÃ¼r den Datensatz %s",attrName, row.get(ReconManagerHelper.COLUMN_RECORDID)),e);
                }
            } else {
                throw new Exception(String.format("%s ist kein Date fÃ¼r den Datensatz %s",attrName, row.get(ReconManagerHelper.COLUMN_RECORDID)));
            }
        }
        return null;
    }

    public static BigDecimal getColumnSum(String col, List<Map<String, Object>> rows, IConditionProcessor<Map<String, Object>> condProcessor, IFormatterProviderProcessor formatterProvider) {
        if (rows == null)
            return BigDecimal.ZERO;
        Assert.notNull(col, "Column name may not be null!");
        Number um;
        IFormatterProcessor formatter = formatterProvider.getFormatterForSubjectReference(col);
        AttributeNumberFormatter numberFormatter = new AttributeNumberFormatter();
        numberFormatter.setPattern("#,##0.###");
        BigDecimal sum = BigDecimal.ZERO;
        for (Map<String, Object> values : rows) {

            try {
                if (condProcessor == null || condProcessor.process(values) != null) {
                    Object oValue = values.get(col.toUpperCase());
                    if (oValue == null) {
                        oValue = BigDecimal.ZERO;
                    }
                    Number n = null;

                    if (oValue instanceof Number) {
                        n = ((Number) oValue);
                    } else if (oValue instanceof String) {
                        try {
                            n = (Number) formatter.parse((String) oValue, Locale.ENGLISH);
                        } catch (Exception e) {
                            // may be the column has no NumberFormatter
                            // (like the
                            // ReconManagerHelper.COLUMN_CALC_VALUE)
                            try {
                                n = (Number) numberFormatter.parse((String) oValue, Locale.ENGLISH);
                            } catch (Exception ex) {
                            }
                        }
                    }

                    if (n != null) {
                        BigDecimal value = null;
                        if (n instanceof BigDecimal)
                            value = (BigDecimal) n;
                        else if (n instanceof Double)
                            value = BigDecimal.valueOf(n.doubleValue());
                        else if (n instanceof Float)
                            value = BigDecimal.valueOf(n.floatValue());
                        else
                            value = BigDecimal.valueOf(n.doubleValue());

                        sum = sum.add(value);
                    }
                }
            } catch (Exception e) {

            }
        }

        return sum;
    }
}