package de.starima.pfw.base.processor.description.config;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.config.api.ITypeRef;
import de.starima.pfw.base.processor.description.config.api.ITypeRefProvider;
import de.starima.pfw.base.processor.description.config.api.ITypeResolutionContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Setter
@Getter
@Slf4j
@Primary
@Processor(description = "Orchestriert eine Kette von ITypeRefProvider-Prozessoren, um jeden beliebigen Java-Typ in eine ITypeRef aufzulÃ¶sen.")
public class TypeRefProviderChain extends AbstractProcessor implements ITypeRefProvider {

    @ProcessorParameter(description = "Die Liste der Provider. Wird zunÃ¤chst durch Spring gesetzt, kann aber auch Ã¼berschrieben werden, wenn dieser Prozessor den" +
            "Initilisierungsprozess durchlÃ¤uft!")
    private List<ITypeRefProvider> providers = List.of();

    //Ein ThreadLocal-Cache. Jeder Thread (d.h. jeder Request) erhÃ¤lt seine eigene Map.
    private final ThreadLocal<Map<Type, ITypeRef>> cache = ThreadLocal.withInitial(ConcurrentHashMap::new);

    /**
     * Konstruktor-basierte Injektion mit ObjectProvider.
     * Spring wird einen ObjectProvider injizieren, der alle ITypeRefProvider-Beans kennt.
     *
     * @param providerProvider Der von Spring bereitgestellte ObjectProvider.
     */
    @Autowired
    public TypeRefProviderChain(ObjectProvider<ITypeRefProvider> providerProvider) {
        // ObjectProvider gibt uns ein Stream-Interface, das wir nutzen kÃ¶nnen,
        // um uns selbst elegant aus der Liste der zu injizierenden Provider herauszufiltern.
        this.providers = providerProvider.stream()
                .filter(provider -> provider != this)
                .collect(Collectors.toList());

        log.info("TypeRefProviderChain initialisiert mit {} externen Providern.", this.providers.size());
    }

    @Override
    public boolean isResponsibleFor(ITypeResolutionContext context) {
        // Die Kette ist immer der erste Ansprechpartner.
        return true;
    }

    @Override
    public ITypeRef provide(ITypeResolutionContext context) {
        // Bootstrap-Logik: Wenn wir der erste Aufrufer sind, verwalten wir den Cache.
        boolean isTopLevelCall = context.getRootProvider() == null;
        if (isTopLevelCall) {
            context.setRootProvider(this);
        }

        try {
            // PrÃ¼fe den Cache, bevor die Provider durchlaufen werden.
            Type typeToResolve = context.getTypeToResolve();
            ITypeRef cachedRef = cache.get().get(typeToResolve);
            if (cachedRef != null) {
                log.trace("Cache-Hit fÃ¼r Typ: {}", typeToResolve.getTypeName());
                return cachedRef;
            }

            // Finde den zustÃ¤ndigen Provider und rufe ihn auf.
            ITypeRef result = providers.stream()
                    .filter(provider -> provider.isResponsibleFor(context))
                    .findFirst()
                    .map(provider -> provider.provide(context))
                    .orElseThrow(() -> new IllegalStateException("Kein ITypeRefProvider fÃ¼r Typ gefunden: " + typeToResolve.getTypeName()));

            // Lege das Ergebnis in den Cache fÃ¼r zukÃ¼nftige Anfragen innerhalb dieses Threads.
            cache.get().put(typeToResolve, result);
            log.trace("Cache-Miss fÃ¼r Typ: {}. Ergebnis gecached.", typeToResolve.getTypeName());

            return result;

        } finally {
            // Wenn dies der Top-Level-Aufruf war, rÃ¤ume den Cache fÃ¼r diesen Thread auf.
            if (isTopLevelCall) {
                log.trace("Top-Level-Aufruf beendet. Cache wird fÃ¼r Thread {} geleert.", Thread.currentThread().getId());
                cache.remove();
            }
        }
    }
}