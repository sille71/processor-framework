package de.starima.pfw.base.processor.attribute.api;

import de.starima.pfw.base.processor.api.IProcessor;

import java.util.List;

public interface IAttributeHelperProcessor<A extends IAttribute,C> extends IProcessor {
    public boolean compareAttribute(A attribute, Object value, C conditionConfig);
    public boolean compareAttributes(A attribute1, A attribute2, C conditionConfig);
    public boolean checkBetween(C conditionConfig, A attribute);
    public List<A> sortAttributes(List<A> attributes);
    public boolean checkIn(C cfg, A attribute);
}