package de.starima.pfw.base.processor.context.api;

import de.starima.pfw.base.processor.api.IProcessor;

/**
 * Löst den zuständigen {@link IRuntimeContextProviderProcessor} für einen gegebenen
 * {@link IContextCreationContext} auf.
 *
 * <p>Folgt dem gleichen Chain-Pattern wie TypeRefProviderChain und DescriptorConfigProviderChain:
 * <b>Resolution</b> (wer ist zuständig?) ist getrennt von <b>Creation</b> (wie wird der Context gebaut?).
 *
 * <p>Der Resolver inspiziert den CreationContext — insbesondere die Prozessorklasse ({@code typeToResolve}),
 * die Prozessor-Instanz ({@code objectToResolve}) und den Parent-Kontext ({@code getRuntimeContext()}) —
 * und entscheidet, ob ein dedizierter ContextProvider konfiguriert wurde.
 *
 * <p>Typischer Ablauf im ProcessorInstanceProvider:
 * <pre>
 *   IRuntimeContextProviderProcessor provider = resolver.resolve(creationContext);
 *   IProcessorContext newCtx = provider.createContext(creationContext);
 * </pre>
 */
public interface IContextProviderResolver extends IProcessor {

    /**
     * Prüft, ob dieser Resolver für den gegebenen CreationContext zuständig ist.
     *
     * @param context Der Kontext mit allen Informationen über den zu erstellenden Prozessor.
     * @return {@code true}, wenn dieser Resolver die Auflösung übernehmen kann.
     */
    boolean isResponsibleFor(IContextCreationContext context);

    /**
     * Löst den zuständigen {@link IRuntimeContextProviderProcessor} auf.
     *
     * <p>Der Resolver prüft:
     * <ol>
     *   <li>Gibt es in den Parametern des Prozessors (aus dem Parent-Kontext) einen
     *       konfigurierten {@code contextProviderProcessor}?</li>
     *   <li>Gibt es in der Default-beanParameterMap der Prozessorklasse einen
     *       konfigurierten {@code contextProviderProcessor}?</li>
     *   <li>Fallback: der ContextProvider des Parent-Kontextes.</li>
     * </ol>
     *
     * @param context Der Kontext mit typeToResolve (Prozessorklasse),
     *                objectToResolve (Prozessor-Instanz) und
     *                getRuntimeContext() (Parent-IProcessorContext).
     * @return Der zuständige ContextProvider. Nie {@code null} —
     *         im Fallback wird der ContextProvider des Parent-Kontextes zurückgegeben.
     */
    IRuntimeContextProviderProcessor resolve(IContextCreationContext context);
}
