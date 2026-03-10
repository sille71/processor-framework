package de.starima.pfw.base.processor.description.incubator;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import de.starima.pfw.base.processor.description.incubator.api.IConstructionManager;
import de.starima.pfw.base.processor.description.incubator.api.IDescriptorResolver;
import de.starima.pfw.base.processor.description.incubator.domain.IBuildTaskContext;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Ein zentraler Orchestrator-Prozessor, der eine Kette von spezialisierten
 * {@link IDescriptorResolver}-Prozessoren verwaltet und den passenden Resolver
 * fr einen gegebenen Build-Kontext auswÃ¤hlt.
 *
 * <p><b>Metapher (Industrie):</b> Dies ist die "Konstrukteursrunde". Der
 * {@link IConstructionManager}
 * (Konstruktionsleiter) fragt diese Runde: "Welcher Spezialist ist fÃ¼r dieses Bauteil zustÃ¤ndig?".</p>
 *
 * <p><b>Metapher (Biologie):</b> Dies ist der "Zellschicksals-Entscheider". Das
 * morphogenetische Feld (ConstructionManager) liefert den Kontext, und diese Kette
 * entscheidet, welcher Zelltyp (Descriptor-Typ) sich hier entwickeln soll.</p>
 *
 * <p>Dieser Prozessor implementiert das "Chain of Responsibility"-Muster. Er iteriert
 * durch die Liste der {@code resolvers} und delegiert die Arbeit an den ersten,
 * der sich fÃ¼r den Kontext zustÃ¤ndig erklÃ¤rt.</p>
 */

@Getter @Setter
@Processor(description = "Orchestriert eine Kette von spezialisierten IDescriptorResolver-Prozessoren, um fÃ¼r jeden Build-Kontext den passenden Descriptor-Knoten zu erzeugen.")
public class DescriptorResolverChain extends AbstractProcessor implements IDescriptorResolver {
    /**
     * Die geordnete Liste der spezialisierten {@link IDescriptorResolver}-Prozessoren.
     * Die Reihenfolge ist entscheidend, da der erste Resolver, der sich Ã¼ber
     * {@code isResponsibleFor} zustÃ¤ndig erklÃ¤rt, den Auftrag erhÃ¤lt.
     * <p>
     * Eine typische Reihenfolge wÃ¤re:
     * <ol>
     *     <li>ReferenceResolver (fÃ¼r Zyklen/Referenzen)</li>
     *     <li>PolymorphicResolver (fÃ¼r Interfaces/abstrakte Klassen)</li>
     *     <li>ProcessorResolver (fÃ¼r @Processor-Klassen)</li>
     *     <li>CollectionResolver (fÃ¼r Listen, Sets, Arrays)</li>
     *     <li>MapResolver (fÃ¼r Maps)</li>
     *     <li>ScalarResolver (fÃ¼r primitive Typen, Strings, Enums, etc.)</li>
     *     <li>StructureResolver (fÃ¼r Standard-ValueObjects)</li>
     * </ol>
     */
    @ProcessorParameter(description = "Die geordnete Liste der spezialisierten Descriptor-Resolver, die die Kette bilden.")
    private List<IDescriptorResolver> resolvers;

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
     * Iteriert durch die Liste der {@code resolvers}, findet den ersten zustÃ¤ndigen
     * Spezialisten und delegiert die Erzeugung des Descriptor-Knotens an diesen.
     *
     * @param ctx Der Build-Kontext, fÃ¼r den ein Descriptor-Knoten erzeugt werden soll.
     * @return Der erzeugte {@link IDescriptorProcessor}-Knoten oder {@code null}, wenn kein
     *         zustÃ¤ndiger Resolver gefunden wurde.
     */
    @Override
    public IDescriptorProcessor resolve(IBuildTaskContext ctx) {
        if (resolvers == null || resolvers.isEmpty()) return null;

        for (IDescriptorResolver resolver : resolvers) {
            if (resolver.isResponsibleFor(ctx)) return resolver.resolve(ctx);
        }
        return null;
    }
}