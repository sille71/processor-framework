package de.starima.pfw.base.processor.description.api;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.description.incubator.api.IInstanceCreationContext;

/**
 * Löst die zuständige {@link IValueFunction} für einen gegebenen Kontext auf.
 *
 * <p>Ersetzt die statische Methode {@code ProcessorUtils.createValueFunctionForContext()}
 * durch eine formale Chain of Responsibility — konsistent mit TypeRefProviderChain,
 * DescriptorConfigProviderChain, ContextProviderResolverChain und InstanceProviderChain.
 *
 * <h3>Resolution-Reihenfolge:</h3>
 * <ol>
 *   <li><b>Annotation:</b> Explizit konfiguriert via {@code @ProcessorParameter(valueFunctionIdentifier=...)}</li>
 *   <li><b>Descriptor:</b> Aus dem Descriptor-System ({@code valueDescriptor.getValueFunction()})</li>
 *   <li><b>Dynamisch:</b> Durchsucht alle registrierten IValueFunction-Beans
 *       ({@code isResponsibleForSubject()})</li>
 * </ol>
 *
 * <h3>Bewusste Entscheidung: Raw Return-Typ</h3>
 * <p>Der Return-Typ ist {@code IValueFunction} (raw, ohne Generics).
 * An jeder Aufrufstelle in ProcessorUtils wird heute bereits zu raw gecastet
 * ({@code @SuppressWarnings("rawtypes")}). Der Resolver bildet das ehrlich ab.
 * Die Generics auf IValueFunction helfen dem <b>Implementierer</b>
 * ({@code AbstractValueFunction<Object, Integer>}), nicht dem <b>Konsumenten</b>.
 *
 * <h3>Keine Komposition:</h3>
 * <p>Die bisherige {@code createValueFunctionForContext()} hat bei Collections/Maps
 * die elementFunction/keyFunction in die gefundene ValueFunction injiziert
 * (ein Seiteneffekt in einer Finder-Methode). Diese Komposition entfällt hier —
 * die Collection/MapInstanceProvider übernehmen die Rekursion über die
 * InstanceProviderChain.
 */
@SuppressWarnings("rawtypes")
public interface IValueFunctionResolver extends IProcessor {

    /**
     * Prüft, ob dieser Resolver für den gegebenen Kontext zuständig ist.
     */
    boolean isResponsibleFor(IInstanceCreationContext context);

    /**
     * Findet die zuständige ValueFunction.
     *
     * @param context Der Kontext mit Typ-Information (typeToResolve, fieldToResolve,
     *                processorParameter, valueDescriptor).
     * @return Die zuständige ValueFunction, oder {@code null} falls keine gefunden.
     */
    IValueFunction resolve(IInstanceCreationContext context);
}
