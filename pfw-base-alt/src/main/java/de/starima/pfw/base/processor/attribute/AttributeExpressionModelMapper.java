package de.starima.pfw.base.processor.attribute;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.attribute.api.IAttribute;
import de.starima.pfw.base.processor.attribute.api.IAttributeModelMapperProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.Map;

@Slf4j
@Getter @Setter
@Processor
public class AttributeExpressionModelMapper extends AbstractProcessor implements IAttributeModelMapperProcessor {
    @ProcessorParameter(parameterFunctionProcessorPrototypeIdentifier = "jacksonParameterFunctionProcessor")
    private Map<String ,String> attributeExpressionMap;

    @Override
    public Object transformValue(IAttribute attribute,Object model) {
        if (!isResponsibleForSubject(attribute)) return model;
        if (model == null) return null;
        if (attributeExpressionMap == null) {
            log.warn("{}: Can not get value for attribute {} from model. No expression map defined!", getIdentifier(), attribute);
            return model;
        }
        String expressonString = attributeExpressionMap.get(attribute.getName());
        if (expressonString == null) {
            log.trace("{}: Can not get value for attribute {} from model. No expression defined in expression map!", getIdentifier(), attribute);
            return model;
        }

        try {
            ExpressionParser parser = new SpelExpressionParser();
            Expression expression = parser.parseExpression(expressonString);
            return expression.getValue(model);
        } catch (ParseException pe) {
            log.warn("{}: Can not parse expression {} for attribute {} from model.", getIdentifier(), expressonString, attribute, pe);
        } catch (EvaluationException evale) {
            log.warn("{}: Can not evaluate expression {} for attribute {} from model.", getIdentifier(), expressonString, attribute, evale);
        }
        return model;
    }

    @Override
    public boolean isResponsibleForSubject(IAttribute subject) {
        return true;
    }
    @Override
    public boolean isResponsibleForInput(Object input) {
        return true;
    }

    @Override
    public Object transformValue(Object input) {
        return null;
    }
}