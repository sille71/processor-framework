package de.starima.pfw.base.processor.description.incubator;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.config.api.IDescriptorConfig;
import de.starima.pfw.base.processor.description.incubator.api.ITypeIntrospector;
import de.starima.pfw.base.processor.description.incubator.domain.IBuildTaskContext;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Ein zentraler Orchestrator-Prozessor, der eine Kette von spezialisierten
 * {@link ITypeIntrospector}-Prozessoren verwaltet. Seine Aufgabe ist es, fÃ¼r
 * einen gegebenen Build-Kontext (der einen Java-Typ oder eine Instanz reprÃ¤sentiert)
 * das passende, lokale {@link IDescriptorConfig}-Fragment zu erzeugen.
 *
 * <p><b>Metapher (Compiler):</b> Dies ist der "Lexer & Parser"-Teil der Pipeline.
 * Er liest den "Quellcode" (Java-Typ, Annotationen, Reflection-Meta) und erzeugt
 * einen Knoten im "Abstract Syntax Tree" (das lokale Config-Fragment).</p>
 *
 * <p><b>Metapher (Biologie):</b> Dies ist der "Gen-Scanner". Er liest einen
 * bestimmten Abschnitt der DNA (Java-Typ/Feld) und extrahiert die darin
 * codierte Information (die Config), ohne bereits die Zelle zu bauen.</p>
 *
 * <p>Dieser Prozessor implementiert das "Chain of Responsibility"-Muster. Er delegiert
 * die Arbeit an den ersten Introspektor in der Kette, der sich fÃ¼r den Kontext
 * zustÃ¤ndig erklÃ¤rt.</p>
 */
@Getter
@Setter
@Processor(
        description = "Orchestriert eine Kette von ITypeIntrospector-Prozessoren, um aus Java-Typen lokale Config-Fragmente zu erzeugen."
)
public class TypeIntrospectorChain extends AbstractProcessor implements ITypeIntrospector {

    /**
     * Die geordnete Liste der spezialisierten {@link ITypeIntrospector}-Prozessoren.
     * Die Reihenfolge ist entscheidend, da der erste Introspektor, der sich zustÃ¤ndig
     * erklÃ¤rt, die Config erzeugt.
     * <p>
     * Eine typische Reihenfolge wÃ¤re:
     * <ol>
     *     <li>PolymorphicTypeIntrospector (fÃ¼r Interfaces/abstrakte Klassen)</li>
     *     <li>ProcessorTypeIntrospector (fÃ¼r @Processor-Klassen)</li>
     *     <li>CollectionTypeIntrospector (fÃ¼r Listen, Sets, Arrays)</li>
     *     <li>MapTypeIntrospector (fÃ¼r Maps)</li>
     *     <li>ScalarTypeIntrospector (fÃ¼r primitive Typen, Strings, Enums, etc.)</li>
     *     <li>StructureTypeIntrospector (fÃ¼r Standard-ValueObjects)</li>
     * </ol>
     */
    @ProcessorParameter(
            description = "Die geordnete Liste der spezialisierten Typ-Introspektoren, die die Kette bilden."
    )
    private List<ITypeIntrospector> introspectors;

    /**
     * Gibt immer {@code true} zurÃ¼ck, da die Kette als Ganzes immer verantwortlich ist,
     * die Anfrage an ihre Glieder weiterzuleiten.
     *
     * @param ctx Der zu prÃ¼fende Build-Kontext.
     * @return immer {@code true}.
     */
    @Override
    public boolean isResponsibleFor(IBuildTaskContext ctx) {
        return true;
    }

    /**
     * Iteriert durch die Liste der {@code introspectors}, findet den ersten zustÃ¤ndigen
     * Spezialisten und delegiert die Erzeugung des lokalen {@link IDescriptorConfig}-Fragments
     * an diesen.
     *
     * @param ctx Der Build-Kontext, der den zu analysierenden Java-Typ enthÃ¤lt.
     * @return Das erzeugte {@link IDescriptorConfig}-Fragment oder {@code null}, wenn kein
     *         zustÃ¤ndiger Introspektor gefunden wurde.
     */
    @Override
    public IDescriptorConfig resolve(IBuildTaskContext ctx) {
        if (introspectors == null || introspectors.isEmpty()) {
            // In einer realen Anwendung wÃ¤re hier ein Warning-Log oder eine Exception sinnvoll.
            return null;
        }

        for (ITypeIntrospector introspector : introspectors) {
            if (introspector.isResponsibleFor(ctx)) {
                return introspector.resolve(ctx);
            }
        }

        // Alternativ eine Exception werfen, um unvollstÃ¤ndige Konfigurationen zu vermeiden:
        // throw new IllegalStateException("Kein zustÃ¤ndiger Introspektor fÃ¼r den Kontext gefunden: " + ctx);
        return null;
    }
}