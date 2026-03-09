package de.starima.pfw.base.processor.condition.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.regex.Pattern;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Getter @Setter
public class ConditionConfig {
    private String columnName;
    private String value;
    private String maxValue;
    private String minValue;
    private String systemVariableName;
    private String compareColumnName;

    private ConditionConfigOperator operator = ConditionConfigOperator.EQ;

    //cache
    @XmlTransient
    @JsonIgnore
    private Pattern pattern;


    @XmlElement(name="opeartor")
    public ConditionConfigOperator getOperator() {
        return operator;
    }

    @Override
    public String toString() {
        return "ConditionConfig [columnName=" + columnName + ", value=" + value
                + ", maxValue=" + maxValue + ", minValue=" + minValue
                + ", operator=" + operator + "]";
    }

    @JsonIgnore
    public boolean isSystemVariable() {
        return systemVariableName != null;
    }

    @XmlTransient
    @JsonIgnore
    public Pattern getPattern() {
        if (ConditionConfigOperator.MATCHES.equals(operator)) {
            pattern = Pattern.compile(value);
            return pattern;
        } else if (ConditionConfigOperator.MATCHES_CASE_INSENSITIVE.equals(operator)) {
            pattern = Pattern.compile(value, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            return pattern;
        }
        return null;
    }

    public enum ConditionConfigOperator {
        EQ {
            @Override
            public String toString() {
                return "=";
            }
        },
        NEQ {
            @Override
            public String toString() {
                return "!=";
            }
        },
        GT {
            @Override
            public String toString() {
                return ">";
            }
        },
        GTE {
            @Override
            public String toString() {
                return ">=";
            }
        },
        LT {
            @Override
            public String toString() {
                return "<";
            }
        },
        LTE {
            @Override
            public String toString() {
                return "<=";
            }
        },
        BETWEEN {
            @Override
            public String toString() {
                return "between";
            }
        },
        TIMESTAMP_BETWEEN {
            @Override
            public String toString() {
                return "timestamp between";
            }
        },
        NULL {
            @Override
            public String toString() {
                return "is null";
            }
        },
        NOT_NULL {
            @Override
            public String toString() {
                return "is not null";
            }
        },
        NOT_EMPTY {
            @Override
            public String toString() {
                return "is not null and not empty";
            }
        },
        IS_EMPTY {
            @Override
            public String toString() {
                return "is null or empty";
            }
        },
        ZERO {
            @Override
            public String toString() {
                return "is null or 0";
            }
        },
        CONTAINS {
            @Override
            public String toString() {
                return "string contains value";
            }
        },
        STARTSWITH {
            @Override
            public String toString() {
                return "startsWith";
            }
        },
        ENDSWITH {
            @Override
            public String toString() {
                return "endsWith";
            }
        },
        MATCHES {
            @Override
            public String toString() {
                return "matches";
            }
        },
        MATCHES_CASE_INSENSITIVE {
            @Override
            public String toString() {
                return "matches";
            }
        },
        IN {
            @Override
            public String toString() {
                return "in";
            }
        },
        ABSMAX {
            @Override
            public String toString() {
                return "absmax";
            }
        },
        GTE_DATE {
            @Override
            public String toString() {
                return "gte_date";
            }
        },
        LTE_DATE {
            @Override
            public String toString() {
                return "lte_date";
            }
        },
        IS_INTEGER {
            @Override
            public String toString() {
                return "is_integer";
            }
        },
        NOT_INTEGER {
            @Override
            public String toString() {
                return "not_integer";
            }
        },
        EQ_DATE {
            @Override
            public String toString() {
                return "eq_date";
            }
        };
    }
}