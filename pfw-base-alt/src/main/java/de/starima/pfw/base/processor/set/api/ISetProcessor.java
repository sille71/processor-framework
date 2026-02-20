package de.starima.pfw.base.processor.set.api;

import de.starima.pfw.base.processor.api.IProcessor;

import java.util.List;

public interface ISetProcessor<E> extends IProcessor {
    /**
     * Gets the membership of an element.
     * @param element - the element to check
     * @return - a double between 0 and 1 (for crisp sets 1 when the element belongs to the set, 0 otherwise)
     *
     * @throws Exception
     */
    public double getMemberShip(E element);

    public boolean isMember(E element);

    public boolean isCollection();

    public List<E> getMembers();
    
    public String getType();

    public String getDomainName();
}