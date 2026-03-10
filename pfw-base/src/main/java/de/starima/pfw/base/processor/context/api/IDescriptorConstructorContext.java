package de.starima.pfw.base.processor.context.api;

import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.Set;

/**
 * Definiert den Kontext fÃ¼r die Erzeugung eines Deskriptors.
 * Dieser Kontext kapselt alle notwendigen Informationen, um sowohl statische "Blueprints"
 * (basierend auf einer Klasse) als auch dynamische "Instance-Deskriptoren" (basierend auf einem Objekt)
 * zu erstellen.
 */
public interface IDescriptorConstructorContext extends ITaskContext {

    /**
     * Das Quell-Feld, fÃ¼r das ein Deskriptor erstellt werden soll.
     * Dies ist die primÃ¤re Quelle fÃ¼r Typinformationen, Annotationen und generische Typen.
     *
     * @return Das zu beschreibende Feld.
     */
    Field getSourceField();

    /**
     * Die konkrete Quell-Instanz, die das Feld enthÃ¤lt.
     * Wenn dieses Objekt vorhanden ist, wird ein "Instance-Deskriptor" erstellt, der
     * den aktuellen Zustand (Wert) des Feldes widerspiegeln kann.
     * Wenn es null ist, wird ein statischer "Blueprint-Deskriptor" erstellt.
     *
     * @return Das Quell-Objekt oder null.
     */
    Object getSourceObject();
    void setSourceObject(Object sourceObject);

    /**
     * Der Typ des Quell-Objekts.
     * Im "Blueprint"-Fall ist dies die Klasse, die beschrieben wird.
     * Im "Instance"-Fall ist dies typischerweise die Klasse von {@link #getSourceObject()}.
     *
     * @return Der Typ der Quell-Klasse.
     */
    Class<?> getSourceType();
    void setSourceType(Class<?> clazz);

    LoadStrategy getLoadStrategy();
    void setLoadStrategy(LoadStrategy loadStrategy);


    /**
     * Der explizit vorgegebene prototype identifier  des zu erstellenden Descriptors.
     * Wenn nicht angegeben, wird er aus getSourceType() erzeugt.
     * @return
     */
    String getDescriptorPrototypeIdentifier();

    ProcessorParameter getProcessorParameter();

    /**
     * Der identifier des ParentDescriptors
     * @return
     */
    String getParentDescriptorIdentifier();

    /**
     * Gibt den Ã¼bergeordneten Deskriptor zurÃ¼ck, falls dieser Kontext innerhalb
     * einer vollstÃ¤ndigen Deskriptor-Hierarchie operiert.
     * Dies ist entscheidend fÃ¼r kompositionale Deskriptoren (z.B. Listen), um auf ihre
     * Kind-Strukturen zugreifen zu kÃ¶nnen.
     *
     * @return Ein Optional, das den Parent-Deskriptor enthÃ¤lt, oder leer ist, wenn im "On-Demand"-Fall operiert wird.
     */
    Optional<IDescriptorProcessor> getParentDescriptor();

    /**
     * Gibt den Ã¼bergeordneten Prozessor zurÃ¼ck, falls dieser Kontext innerhalb einer
     * Hierarchie von "lebenden" Prozessor-Instanzen operiert.
     *
     * @return Ein Optional, das den Parent-Prozessor enthÃ¤lt.
     */
    Optional<IProcessor> getParentProcessor();
    void setParentProcessor(Optional<IProcessor> parentProcessor);

    /**
     * Gibt das Set der Typen zurÃ¼ck, die auf dem aktuellen Rekursionspfad
     * bereits besucht wurden, um Zyklen zu erkennen.
     */
    Set<Class<?>> getVisitedTypes();

    /**
     * Setzt das Set der besuchten Typen.
     */
    void setVisitedTypes(Set<Class<?>> visitedTypes);
}