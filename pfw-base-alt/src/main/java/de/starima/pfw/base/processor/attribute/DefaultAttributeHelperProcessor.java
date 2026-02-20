package de.starima.pfw.base.processor.attribute;

import de.dzbank.components.utils.BeanUtilities;
import de.starima.pfw.base.processor.formatter.api.IFormatterProcessor;
import de.starima.pfw.base.processor.formatter.api.IFormatterProviderProcessor;
import de.starima.pfw.base.processor.locale.api.ILocalProviderProcessor;
import de.starima.pfw.base.util.CalculationHelper;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.attribute.api.IAttributeHelperProcessor;
import de.starima.pfw.base.processor.attribute.domain.RcnAttribute;
import de.starima.pfw.base.processor.condition.domain.ConditionConfig;
import de.starima.pfw.base.processor.locale.api.ILocalDateTimeProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

@Getter
@Setter
@Slf4j
@Processor
public class DefaultAttributeHelperProcessor extends AbstractProcessor implements IAttributeHelperProcessor<RcnAttribute, ConditionConfig> {
    @ProcessorParameter(value = "defaultLocalDateTimeProcessor")
    private ILocalDateTimeProcessor localDateTimeProcessor;
    @ProcessorParameter(value = "defaultAttributeFormatterProvider")
    private IFormatterProviderProcessor formatterProviderProcessor;
    @ProcessorParameter(value = "defaultLocalProviderProcessor")
    private ILocalProviderProcessor localProviderProcessor;

    private String format(RcnAttribute attribute) {
        Assert.notNull(this.formatterProviderProcessor, "no formatterProvider defined!");
        Assert.notNull(this.localProviderProcessor, "no localProviderProcessor defined!");
        if (attribute == null) return null;

        IFormatterProcessor fp = this.formatterProviderProcessor.getFormatterForSubject(attribute);
        if (fp == null) {
            log.error("{}.format no formatter found for attribute {}!", this.getIdentifier(), attribute.getName());
            return null;
        }
        return fp.format(attribute.getValue(), attribute, localProviderProcessor.getLocaleForSubject(attribute));
    }

    private Object parse(RcnAttribute attribute, String value) {
        Assert.notNull(this.formatterProviderProcessor, "no formatterProvider defined!");
        Assert.notNull(this.localProviderProcessor, "no localProviderProcessor defined!");
        if (attribute == null) return null;

        IFormatterProcessor fp = this.formatterProviderProcessor.getFormatterForSubject(attribute);
        if (fp == null) {
            log.error("{}.parse no formatter found for attribute {}!", this.getIdentifier(), attribute.getName());
            return null;
        }
        try {
            return fp.parse(value, attribute, localProviderProcessor.getLocaleForSubject(attribute));
        } catch (ParseException e) {
            log.error("{}.parse can not parse value {} for attribute {}!", this.getIdentifier(), value, attribute.getName());
            return null;
        }
    }

    @Override
    public boolean compareAttribute(RcnAttribute attribute, Object value, ConditionConfig cfg) {
        switch (cfg.getOperator()) {
            case EQ:
                try {
                    log.trace("EQ: Comparing column {} with columnValue {} to value {}", attribute.getName(), attribute.getValue(), value);
                    return ReconManagerHelper.compareObjects(attribute.getValue(), value) == 0;
                } catch (Exception e) {
                    log.warn("EQ: Can not compare attribute {} with attributeValue {} to value {} failed. Msg: {}",
                            attribute.getName(), attribute.getValue(), value, e.toString());
                    return false;
                }
            case GT:
                try {
                    log.trace("GT: Comparing attribute {} with attributeValue {} to value {}", attribute.getName(), attribute.getValue(), value);
                    return ReconManagerHelper.compareObjects(attribute.getValue(), value) > 0;
                } catch (Exception e) {
                    log.warn("GT: Can not compare attribute {} with attributeValue {} to value {} failed. Msg: {}",
                            attribute.getName(), attribute.getValue(), value, e.toString());
                    return false;
                }
            case GTE:
                try {
                    log.trace("GTE: Comparing attribute {} with attributeValue {} to value {}", attribute.getName(), attribute.getValue(), value);
                    return (ReconManagerHelper.compareObjects(attribute.getValue(), value) > 0)
                            || (ReconManagerHelper.compareObjects(attribute.getValue(), value) == 0);
                } catch (Exception e) {
                    log.warn("GTE: Can not compare attribute {} with attributeValue {} to value {} failed. Msg: {}",
                            attribute.getName(), attribute.getValue(), value, e.toString());
                    return false;
                }
            case LT:
                try {
                    log.trace("LT: Comparing attribute {} with attributeValue {} to value {}", attribute.getName(), attribute.getValue(), value);
                    return ReconManagerHelper.compareObjects(attribute.getValue(), value) < 0;
                } catch (Exception e) {
                    log.warn("LT: Can not compare attribute {} with attributeValue {} to value {} failed. Msg: {}",
                            attribute.getName(), attribute.getValue(), value, e.toString());
                    return false;
                }
            case LTE:
                try {
                    log.trace("LTE: Comparing attribute {} with attributeValue {} to value {}", attribute.getName(), attribute.getValue(), value);
                    return (ReconManagerHelper.compareObjects(attribute.getValue(), value) < 0)
                            || (ReconManagerHelper.compareObjects(attribute.getValue(), value) == 0);
                } catch (Exception e) {
                    log.warn("LTE: Can not compare attribute {} with attributeValue {} to value {} failed. Msg: {}",
                            attribute.getName(), attribute.getValue(), value, e.toString());
                    return false;
                }
            case CONTAINS:
                try {
                    //cfg.getValue may be a comma separated list of strings!
                    //we will check attribute.getValue().contains against each of these strings
                    //get the string representation of the attribute value
                    log.trace("CONTAINS: Comparing attribute {} with attributeValue {} to value {}", attribute.getName(), attribute.getValue(), value);

                    String stringRep = format(attribute);
                    if (stringRep == null) return false;

                    return ReconManagerHelper.checkStringAction(cfg.getValue(), stringRep::contains);
                } catch (Exception e) {
                    log.warn("CONTAINS: Can not compare attribute {} with attributeValue {} to value {} failed. Msg: {}",
                            attribute.getName(), attribute.getValue(), value, e.toString());
                    return false;
                }
            case STARTSWITH:
                try {
                    //cfg.getValue may be a comma separated list of strings!
                    //we will check attribute.getValue().startsWith against each of these strings
                    //get the string representation of the attribute value
                    log.trace("STARTSWITH: Comparing attribute {} with attributeValue {} to value {}", attribute.getName(), attribute.getValue(), value);
                    String stringRep = format(attribute);
                    if (stringRep == null) return false;

                    return ReconManagerHelper.checkStringAction(cfg.getValue(), stringRep::startsWith);
                } catch (Exception e) {
                    log.warn("STARTSWITH: Can not compare attribute {} with attributeValue {} to value {} failed. Msg: {}",
                            attribute.getName(), attribute.getValue(), value, e.toString());
                    return false;
                }
            case ENDSWITH:
                try {
                    //cfg.getValue may be a comma separated list of strings!
                    //we will check attribute.getValue().endsWith against each of these strings
                    //get the string representation of the attribute value
                    log.trace("ENDSWITH: Comparing attribute {} with attributeValue {} to value {}", attribute.getName(), attribute.getValue(), value);
                    String stringRep = format(attribute);
                    if (stringRep == null) return false;

                    return ReconManagerHelper.checkStringAction(cfg.getValue(), stringRep::endsWith);
                } catch (Exception e) {
                    log.warn("ENDSWITH: Can not compare attribute {} with attributeValue {} to value {} failed. Msg: {}",
                            attribute.getName(), attribute.getValue(), value, e.toString());
                    return false;
                }
            case NEQ:
                try {
                    log.trace("NEQ: Comparing attribute {} with attributeValue {} to value {}", attribute.getName(), attribute.getValue(), value);
                    return ReconManagerHelper.compareObjects(attribute.getValue(), value) != 0;
                } catch (Exception e) {
                    log.warn("NEQ: Can not compare attribute {} with attributeValue {} to value {} failed. Msg: {}",
                            attribute.getName(), attribute.getValue(), value, e.toString());
                    return false;
                }
            case NULL:
                log.trace("NULL: Comparing attribute {} with attributeValue {} to value {}", attribute.getName(), attribute.getValue(), value);
                return attribute.getValue() == null;
            case NOT_NULL:
                log.trace("NOT_NULL: Comparing attribute {} with attributeValue {} to value {}", attribute.getName(), attribute.getValue(), value);
                return attribute.getValue() != null;
            case ZERO:
                try {
                    log.trace("ZERO: Comparing attribute {} with attributeValue {} to value {}", attribute.getName(), attribute.getValue(), value);
                    if (attribute.isNumeric()) {
                        if (attribute.getValue() == null)
                            return true;

                        Number nv = (Number) attribute.getValue();
                        return nv.doubleValue() == 0.0;
                    }
                    return false;
                } catch (Exception e) {
                    log.warn("ZERO: Can not compare attribute {} with attributeValue {} to value {} failed. Msg: {}",
                            attribute.getName(), attribute.getValue(), value, e.toString());
                    return false;
                }
            case MATCHES:
                try {
                    log.trace("MATCHES: Comparing attribute {} with attributeValue {} to value {}", attribute.getName(), attribute.getValue(), value);
                    Pattern pattern = cfg.getPattern();
                    String value1 = format(attribute);
                    return pattern != null && value1 != null && pattern.matcher(value1).matches();
                } catch (Exception e) {
                    log.warn("MATCHES: Can not match regex {} against attribute {} Msg: {}", cfg.getValue(), attribute.getName(), e.toString());
                    return false;
                }
            case MATCHES_CASE_INSENSITIVE:
                try {
                    log.trace("MATCHES_CASE_INSENSITIVE: Comparing attribute {} with attributeValue {} to value {}", attribute.getName(), attribute.getValue(), value);
                    Pattern pattern = cfg.getPattern();
                    String value1 = format(attribute);
                    return pattern != null && value1 != null && pattern.matcher(value1).matches();
                } catch (Exception e) {
                    log.warn("MATCHES_CASE_INSENSITIVE: Can not match regex {} against attribute {} Msg: {}", cfg.getValue(), attribute.getName(), e.toString());
                    return false;
                }
            case BETWEEN:
                try {
                    log.trace("BETWEEN: Comparing attribute {} with attributeValue {} to value {}", attribute.getName(), attribute.getValue(), value);
                    return this.checkBetween(cfg, attribute);
                } catch (Exception e) {
                    log.warn("BETWEEN: Can not compare attribute {} with attributeValue {} to value {} failed. Msg: {}",
                            attribute.getName(), attribute.getValue(), value, e.toString());
                    return false;
                }
            case IN:
                try {
                    log.trace("IN: Comparing attribute {} with attributeValue {} to value {}", attribute.getName(), attribute.getValue(), value);
                    return this.checkIn(cfg, attribute);
                } catch (Exception e) {
                    log.warn("IN: Can not compare attribute {} with attributeValue {} to value {} failed. Msg: {}",
                            attribute.getName(), attribute.getValue(), value, e.toString());
                    return false;
                }
            case ABSMAX:
                try {
                    log.trace("ABSMAX: Comparing attribute {} with attributeValue {} to value {}", attribute.getName(), attribute.getValue(), value);
                    if (attribute.isNumeric()) {
                        double tol = 0.0;
                        try {
                            tol = Double.parseDouble(cfg.getValue());
                        } catch (NumberFormatException e) {
                            log.error("ABSMAX, cfg.value {} is not a double!", cfg.getValue());
                        }

                        return CalculationHelper.absDifferenceNumbers((Number)attribute.getValue(), 0.0).doubleValue() <= Math.abs(tol);
                    }

                    return false;
                } catch (Exception e) {
                    log.warn("ABSMAX: Can not compare attribute {} with attributeValue {} to value {} failed. Msg: {}",
                            attribute.getName(), attribute.getValue(), value, e.toString());
                    return false;
                }
            case IS_INTEGER:
                try {
                    log.trace("IS_INTEGER: Comparing attribute {} with attributeValue {} to value {}", attribute.getName(), attribute.getValue(), value);
                    if (attribute.isNumeric()) {
                        if (attribute.getValue() == null)
                            return false;
                        return ((Number)attribute.getValue()).doubleValue() - ((Number)attribute.getValue()).intValue() == 0;
                    }

                    return false;
                } catch (Exception e) {
                    log.warn("IS_INTEGER: Can not compare attribute {} with attributeValue {} to value {} failed. Msg: {}",
                            attribute.getName(), attribute.getValue(), value, e.toString());
                    return false;
                }
            case NOT_INTEGER:
                try {
                    log.trace("NOT_INTEGER: Comparing attribute {} with attributeValue {} to value {}", attribute.getName(), attribute.getValue(), value);
                    if (attribute.isNumeric()) {
                        if (attribute.getValue() == null)
                            return false;
                        return ((Number)attribute.getValue()).doubleValue() - ((Number)attribute.getValue()).intValue() != 0;
                    }

                    return false;
                } catch (Exception e) {
                    log.warn("NOT_INTEGER: Can not compare attribute {} with attributeValue {} to value {} failed. Msg: {}",
                            attribute.getName(), attribute.getValue(), value, e.toString());
                    return false;
                }
            case NOT_EMPTY:
                try {
                    log.trace("NOT_EMPTY: Comparing attribute {} with attributeValue {} to value {}", attribute.getName(), attribute.getValue(), value);
                    if (attribute.isString())
                        return attribute.getValue() != null && !((String)attribute.getValue()).isEmpty();

                    return attribute.getValue() != null;
                } catch (Exception e) {
                    log.warn("NOT_EMPTY: Can not compare attribute {} with attributeValue {} to value {} failed. Msg: {}",
                            attribute.getName(), attribute.getValue(), value, e.toString());
                    return false;
                }
            case IS_EMPTY:
                try {
                    log.trace("IS_EMPTY: Comparing attribute {} with attributeValue {} to value {}", attribute.getName(), attribute.getValue(), value);
                    if (attribute.isString())
                        return attribute.getValue() == null || ((String)attribute.getValue()).isEmpty();

                    return attribute.getValue() == null;
                } catch (Exception e) {
                    log.warn("IS_EMPTY: Can not compare attribute {} with attributeValue {} to value {} failed. Msg: {}",
                            attribute.getName(), attribute.getValue(), value, e.toString());
                    return false;
                }
            default:
                log.warn("Can not compare attribute {} with attributeValue {} to value {}! No suitable operator found.",
                        attribute.getName(), attribute.getValue(), value);
                return false;
        }
    }

    @Override
    public boolean compareAttributes(RcnAttribute column1, RcnAttribute column2, ConditionConfig cfg) {
        switch (cfg.getOperator()) {
            case EQ:
                try {
                    log.trace("EQ: Comparing column {} with columnValue {} to column {} with columnValue {}",
                            column1.getName(), column1.getValue(), column2.getName(), column2.getValue());
                    return ReconManagerHelper.compareObjects(column1.getValue(), column2.getValue()) == 0;
                } catch (Exception e) {
                    log.warn("EQ: Can not compare column {} with columnValue {} to column {} with columnValue {}. Msg: {}",
                            column1.getName(), column1.getValue(), column2.getName(), column2.getValue(), e.toString());
                    return false;
                }
            case GT:
                try {
                    log.trace("GT: Comparing column {} with columnValue {} to column {} with columnValue {}",
                            column1.getName(), column1.getValue(), column2.getName(), column2.getValue());
                    return ReconManagerHelper.compareObjects(column1.getValue(), column2.getValue()) > 0;
                } catch (Exception e) {
                    log.warn("GT: Can not compare column {} with columnValue {} to column {} with columnValue {}. Msg: {}",
                            column1.getName(), column1.getValue(), column2.getName(), column2.getValue(), e.toString());
                    return false;
                }
            case GTE:
                try {
                    log.trace("GTE: Comparing column {} with columnValue {} to column {} with columnValue {}",
                            column1.getName(), column1.getValue(), column2.getName(), column2.getValue());
                    return (ReconManagerHelper.compareObjects(column1.getValue(), column2.getValue()) > 0)
                            || (ReconManagerHelper.compareObjects(column1.getValue(), column2.getValue()) == 0);
                } catch (Exception e) {
                    log.warn("GTE: Can not compare column {} with columnValue {} to column {} with columnValue {}. Msg: {}",
                            column1.getName(), column1.getValue(), column2.getName(), column2.getValue(), e.toString());
                    return false;
                }
            case LT:
                try {
                    log.trace("LT: Comparing column {} with columnValue {} to column {} with columnValue {}",
                            column1.getName(), column1.getValue(), column2.getName(), column2.getValue());
                    return ReconManagerHelper.compareObjects(column1.getValue(), column2.getValue()) < 0;
                } catch (Exception e) {
                    log.warn("LT: Can not compare column {} with columnValue {} to column {} with columnValue {}. Msg: {}",
                            column1.getName(), column1.getValue(), column2.getName(), column2.getValue(), e.toString());
                    return false;
                }
            case LTE:
                try {
                    log.trace("LTE: Comparing column {} with columnValue {} to column {} with columnValue {}",
                            column1.getName(), column1.getValue(), column2.getName(), column2.getValue());
                    return (ReconManagerHelper.compareObjects(column1.getValue(), column2.getValue()) < 0)
                            || (ReconManagerHelper.compareObjects(column1.getValue(), column2.getValue()) == 0);
                } catch (Exception e) {
                    log.warn("LTE: Can not compare column {} with columnValue {} to column {} with columnValue {}. Msg: {}",
                            column1.getName(), column1.getValue(), column2.getName(), column2.getValue(), e.toString());
                    return false;
                }
            case CONTAINS:
                try {
                    log.trace("CONTAINS: Comparing column {} with columnValue {} to column {} with columnValue {}",
                            column1.getName(), column1.getValue(), column2.getName(), column2.getValue());

                    String value1 = this.format(column1);
                    String value2 = this.format(column2);
                    return value1 != null && value2 != null && value1.contains(value2);
                } catch (Exception e) {
                    log.warn("CONTAINS: Can not compare column {} with columnValue {} to column {} with columnValue {}. Msg: {}",
                            column1.getName(), column1.getValue(), column2.getName(), column2.getValue(), e.toString());
                    return false;
                }
            case STARTSWITH:
                try {
                    log.trace("STARTSWITH: Comparing column {} with columnValue {} to column {} with columnValue {}",
                            column1.getName(), column1.getValue(), column2.getName(), column2.getValue());
                    String value1 = this.format(column1);
                    String value2 = this.format(column2);
                    return value1 != null && value2 != null && value1.startsWith(value2);
                } catch (Exception e) {
                    log.warn("STARTSWITH: Can not compare column {} with columnValue {} to column {} with columnValue {}. Msg: {}",
                            column1.getName(), column1.getValue(), column2.getName(), column2.getValue(), e.toString());
                    return false;
                }
            case ENDSWITH:
                try {
                    log.trace("ENDSWITH: Comparing column {} with columnValue {} to column {} with columnValue {}",
                            column1.getName(), column1.getValue(), column2.getName(), column2.getValue());
                    String value1 = this.format(column1);
                    String value2 = this.format(column2);
                    return value1 != null && value2 != null && value1.endsWith(value2);
                } catch (Exception e) {
                    log.warn("ENDSWITH: Can not compare column {} with columnValue {} to column {} with columnValue {}. Msg: {}",
                            column1.getName(), column1.getValue(), column2.getName(), column2.getValue(), e.toString());
                    return false;
                }
            case NEQ:
                try {
                    log.trace("NEQ: Comparing column {} with columnValue {} to column {} with columnValue {}",
                            column1.getName(), column1.getValue(), column2.getName(), column2.getValue());
                    return ReconManagerHelper.compareObjects(column1.getValue(), column2.getValue()) != 0;
                } catch (Exception e) {
                    log.warn("NEQ: Can not compare column {} with columnValue {} to column {} with columnValue {}. Msg: {}",
                            column1.getName(), column1.getValue(), column2.getName(), column2.getValue(), e.toString());
                    return false;
                }
            case ABSMAX:
                try {
                    log.trace("ABSMAX: Comparing column {} with columnValue {} to column {} with columnValue {}",
                            column1.getName(), column1.getValue(), column2.getName(), column2.getValue());
                    if ((column1.isDate() || column1.isTimestamp()) && (column2.isDate() || column2.isTimestamp())) {
                        LocalDate date1 = localDateTimeProcessor.toLocalDate((Date) column1.getValue());
                        LocalDate date2 = localDateTimeProcessor.toLocalDate((Date) column2.getValue());

                        int diff = 0;
                        try {
                            diff = Integer.parseInt(cfg.getValue());
                        } catch (NumberFormatException e) {
                            log.error("ABSMAX, cfg.value {} is not an integer!", cfg.getValue());
                        }

                        return Math.abs(localDateTimeProcessor.daysBetween(date1, date2)) <= Math.abs(diff);
                    } else if (column1.isNumeric() && column2.isNumeric()) {
                        double tol = 0.0;
                        try {
                            tol = Double.parseDouble(cfg.getValue());
                        } catch (NumberFormatException e) {
                            log.error("ABSMAX, cfg.value {} is not a double!", cfg.getValue());
                        }

                        return CalculationHelper.absDifferenceNumbers((Number)column1.getValue(), (Number)column2.getValue()).doubleValue() <= Math.abs(tol);
                    }

                    return false;
                } catch (Exception e) {
                    log.warn("ABSMAX: Can not compare column {} with columnValue {} to column {} with columnValue {}. Msg: {}",
                            column1.getName(), column1.getValue(), column2.getName(), column2.getValue(), e.toString());
                    return false;
                }
            case GTE_DATE:
                try {
                    log.trace("GTE_DATE: Comparing column {} with columnValue {} to column {} with columnValue {}",
                            column1.getName(), column1.getValue(), column2.getName(), column2.getValue());
                    if ((column1.isDate() || column1.isTimestamp()) && (column2.isDate() || column2.isTimestamp())) {
                        LocalDate date1 = localDateTimeProcessor.toLocalDate((Date) column1.getValue());
                        LocalDate date2 = localDateTimeProcessor.toLocalDate((Date) column2.getValue());

                        return !date1.isBefore(date2);
                    }

                    return false;
                } catch (Exception e) {
                    log.warn("GTE_DATE: Can not compare column {} with columnValue {} to column {} with columnValue {}. Msg: {}",
                            column1.getName(), column1.getValue(), column2.getName(), column2.getValue(), e.toString());
                    return false;
                }
            case LTE_DATE:
                try {
                    log.trace("LTE_DATE: Comparing column {} with columnValue {} to column {} with columnValue {}",
                            column1.getName(), column1.getValue(), column2.getName(), column2.getValue());
                    if ((column1.isDate() || column1.isTimestamp()) && (column2.isDate() || column2.isTimestamp())) {
                        LocalDate date1 = localDateTimeProcessor.toLocalDate((Date) column1.getValue());
                        LocalDate date2 = localDateTimeProcessor.toLocalDate((Date) column2.getValue());

                        return !date1.isAfter(date2);
                    }

                    return false;
                } catch (Exception e) {
                    log.warn("LTE_DATE: Can not compare column {} with columnValue {} to column {} with columnValue {}. Msg: {}",
                            column1.getName(), column1.getValue(), column2.getName(), column2.getValue(), e.toString());
                    return false;
                }
            case EQ_DATE:
                try {
                    log.trace("EQ_DATE: Comparing column {} with columnValue {} to column {} with columnValue {}",
                            column1.getName(), column1.getValue(), column2.getName(), column2.getValue());
                    if ((column1.isDate() || column1.isTimestamp()) && (column2.isDate() || column2.isTimestamp())) {
                        return ReconManagerHelper.compareObjects(CalculationHelper.toDate(column1.getValue()), CalculationHelper.toDate(column2.getValue())) == 0;
                    }

                    return false;
                } catch (Exception e) {
                    log.warn("EQ_DATE: Can not compare column {} with columnValue {} to column {} with columnValue {}. Msg: {}",
                            column1.getName(), column1.getValue(), column2.getName(), column2.getValue(), e.toString());
                    return false;
                }
            default:
                log.warn("Can not compare column {} with columnValue {} to column {} with columnValue {}! No suitable Oparator found.",
                        column1.getName(), column1.getValue(), column2.getName(), column2.getValue());
                return false;
        }
    }

    public List<RcnAttribute> sortAttributes(List<RcnAttribute> attributes) {
        if (attributes == null) return new ArrayList<>();
        //falls ein Attribute keine definierte sortOrder hat nehmen wir die Reihenfolge der Liste als sortOrder und Ã¼berschreiben die Attribut sortOrder Werte
        if (attributes.stream().anyMatch(attr -> attr.getSortOrder() == null)) {
            int index = 0;
            for (RcnAttribute attr : attributes) {
                attr.setSortOrder(index);
                index++;
            }
        }

        return BeanUtilities.sortBeanList(attributes, "sortOrder", false);
    }

    public boolean checkBetween(ConditionConfig cfg, RcnAttribute attribute) {
        try {

            Object itemValue = attribute.getValue();

            if (attribute.isNumeric()) {
                BigDecimal maxValue;
                BigDecimal minValue;

                try {
                    Number nv = (Number) parse(attribute, cfg.getMaxValue());
                    assert nv != null;
                    maxValue = BigDecimal.valueOf(nv.doubleValue());
                } catch (Exception e) {
                    log.error("{}.checkBetween(): Can not parse number attribute {} for maxvalue {}!",this.getIdentifier() ,attribute.getName(), cfg.getMaxValue(), e);
                    return false;
                }

                try {
                    Number nv = (Number) parse(attribute, cfg.getMinValue());
                    assert nv != null;
                    minValue = BigDecimal.valueOf(nv.doubleValue());
                } catch (Exception e) {
                    log.error("{}.checkBetween(): Can not parse number attribute {} for minvalue {}!",this.getIdentifier() ,attribute.getName(), cfg.getMinValue(), e);
                    return false;
                }

                if (itemValue != null) {
                    BigDecimal value = BigDecimal.valueOf(((Number) itemValue).doubleValue());
                    return minValue.compareTo(value) < 0 && value.compareTo(maxValue) < 0;
                }
                return false;
            } else if (attribute.isDate() || attribute.isTimestamp()) {
                Date maxValue;
                Date minValue;

                try {
                    maxValue = (Date) parse(attribute, cfg.getMaxValue());
                } catch (Exception e) {
                    log.error("{}.checkBetween(): Can not parse date attribute {} for maxvalue {}!",this.getIdentifier() ,attribute.getName(), cfg.getMaxValue(), e);
                    return false;
                }

                try {
                    minValue = (Date) parse(attribute, cfg.getMinValue());
                } catch (Exception e) {
                    log.error("{}.checkBetween(): Can not parse date attribute {} for minvalue {}!",this.getIdentifier() ,attribute.getName(), cfg.getMinValue(), e);
                    return false;
                }

                if (itemValue != null && maxValue != null && minValue != null) {
                    Date value = (Date) itemValue;
                    return minValue.compareTo(value) < 0 && value.compareTo(maxValue) < 0;
                }
                return false;
            }
        } catch (Exception e) {
            log.error("{}.checkBetween(): Can not check between for attribute {} with value {}!",this.getIdentifier() ,attribute.getName(), cfg.getValue(), e);
        }
        return false;
    }

    public boolean checkIn(ConditionConfig cfg, RcnAttribute attribute) {
        try {
            if (cfg.getValue() == null)
                return false;
            if (attribute == null) return false;

            // assume the value is the comma separated list of string
            // representations
            Object parsedValue;
            for (String s : cfg.getValue().split(",")) {
                parsedValue = this.parse(attribute, s);
                if (ReconManagerHelper.compareObjects(attribute.getValue(), parsedValue) == 0)
                    return true;
            }
        } catch (Exception e) {
            log.error("{}.checkIn(): Can not check IN for attribute {} with value {}!",this.getIdentifier() ,attribute.getName(), cfg.getValue(), e);
        }
        return false;
    }
}