package de.starima.pfw.base.processor.description.incubator.api;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import de.dzbank.recon.ms.base.processor.description.incubator.domain.*;
import de.starima.pfw.base.processor.description.incubator.domain.*;

public interface IIncubator extends IProcessor {



    IDescribeSession startDescribe(IDescribeSource source, IDescribePolicy policy);
    default IDescriptorProcessor describe(IDescribeSource source, IDescribePolicy policy) {
        var session = startDescribe(source, policy);
        return session != null ? session.getRoot() : null;
    }
    IConstructSession<Object> startConstruct(IConstructSource source, IConstructPolicy policy);
    <T> IConstructSession<T> startConstruct(Class<T> clazz, IConstructSource source, IConstructPolicy policy);
    default Object construct(IConstructSource source, IConstructPolicy policy) {
        var session = startConstruct(source, policy);
        return session != null ? session.getRoot() : null;
    }
    default <T> T construct(Class<T> clazz, IConstructSource source, IConstructPolicy policy) {
        var session = startConstruct(clazz, source, policy);
        return session != null ? session.getRoot() : null;
    }

    IEditSession startEdit(IEditSource source, IEditPolicy policy);
}