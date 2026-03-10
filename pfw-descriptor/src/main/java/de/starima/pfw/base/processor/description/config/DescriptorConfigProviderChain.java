package de.starima.pfw.base.processor.description.config;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.config.api.IDescriptorConfig;
import de.starima.pfw.base.processor.description.config.api.IDescriptorConfigCreationContext;
import de.starima.pfw.base.processor.description.config.api.IDescriptorConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Primary
@Processor(description = "Orchestriert eine Kette von IDescriptorConfigProvider-Prozessoren, um Konfigurations-Deskriptoren zu erzeugen.")
public class DescriptorConfigProviderChain extends AbstractProcessor implements IDescriptorConfigProvider {

    private final List<IDescriptorConfigProvider> providers;

    @Autowired
    public DescriptorConfigProviderChain(ObjectProvider<IDescriptorConfigProvider> providerProvider) {
        this.providers = providerProvider.stream()
                .filter(provider -> provider != this)
                .collect(Collectors.toList());
        log.info("DescriptorConfigProviderChain initialisiert mit {} externen Providern.", this.providers.size());
    }

    @Override
    public boolean isResponsibleFor(IDescriptorConfigCreationContext context) {
        return true;
    }

    @Override
    public IDescriptorConfig provide(IDescriptorConfigCreationContext context) {
        if (context.getRootProvider() == null) {
            context.setRootProvider(this);
        }

        return providers.stream()
                .filter(provider -> provider.isResponsibleFor(context))
                .findFirst()
                .map(provider -> provider.provide(context))
                .orElse(null);
    }
}
