package de.starima.pfw.base.processor.description.incubator;

import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.incubator.api.IConstructSession;
import de.starima.pfw.base.processor.description.incubator.api.IDescribeSession;
import de.starima.pfw.base.processor.description.incubator.api.IEditSession;
import de.starima.pfw.base.processor.description.incubator.api.IIncubator;
import de.dzbank.recon.ms.base.processor.description.incubator.domain.*;
import de.starima.pfw.base.processor.description.incubator.domain.*;

public class FrameworkIncubator extends AbstractProcessor implements IIncubator {
    @Override
    public IDescribeSession startDescribe(IDescribeSource source, IDescribePolicy policy) {
        return null;
    }

    @Override
    public IConstructSession<Object> startConstruct(IConstructSource source, IConstructPolicy policy) {
        return null;
    }

    @Override
    public <T> IConstructSession<T> startConstruct(Class<T> clazz, IConstructSource source, IConstructPolicy policy) {
        return null;
    }

    @Override
    public IEditSession startEdit(IEditSource source, IEditPolicy policy) {
        return null;
    }
}