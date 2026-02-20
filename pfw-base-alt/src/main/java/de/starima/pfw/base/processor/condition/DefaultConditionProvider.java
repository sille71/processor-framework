package de.starima.pfw.base.processor.condition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.condition.api.IConditionProviderProcessor;
import de.starima.pfw.base.processor.condition.domain.Condition;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter @Setter
@Processor
public class DefaultConditionProvider extends AbstractProcessor implements IConditionProviderProcessor<Condition> {

    @ProcessorParameter(parameterFunctionProcessorPrototypeIdentifier = "jacksonParameterFunctionProcessor")
    List<Condition> conditions;

    @Override
    public Condition getCondition(String conditionReference) {
        if (conditionReference == null || conditionReference.isBlank()) return null;
        if (conditions != null) {
            try {
                return conditions.stream().filter(condition -> conditionReference.equalsIgnoreCase(condition.getIdentifier())).findFirst().get();
            } catch (Exception e) {
                log.warn("{}: Can not find column {}", this.getIdentifier(), conditionReference);
            }
        }

        return null;
    }

    @Override
    public List<Condition> getConditions() {
        return conditions;
    }

    @Override
    public List<Condition> getConditions(List<String> conditionReferences) {
        if (getConditions() != null && conditionReferences != null) {
            ArrayList<Condition> result = new ArrayList<>();
            conditionReferences.forEach(identifier -> result.add(getCondition(identifier)));
            return result;
        }

        return new ArrayList<>();
    }

    @Override
    public List<String> getConditionReferences() {
        if (conditions != null) {
            try {
                return conditions.stream().collect(new ConditionNameCollector());
            } catch (Exception e) {
                log.warn("{}: Can not collect condition identifiers", this.getIdentifier());
            }
        }
        return new ArrayList<>();
    }

    private class ConditionNameCollector implements Collector<Condition, List<String>, List<String>> {

        @Override
        public Supplier<List<String>> supplier() {
            return () -> new ArrayList<>();
        }

        @Override
        public BiConsumer<List<String>, Condition> accumulator() {
            return (list, condition) -> list.add(condition.getIdentifier());
        }

        @Override
        public BinaryOperator<List<String>> combiner() {
            return (listA, listB) -> {listA.addAll(listB); return listA;};
        }

        @Override
        public Function<List<String>, List<String>> finisher() {
            return Function.identity();
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Collections.emptySet();
        }
    }
}