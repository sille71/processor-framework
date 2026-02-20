package de.starima.pfw.base.processor.description.config;

import de.starima.pfw.base.processor.context.domain.DefaultTaskContext;
import de.starima.pfw.base.processor.description.config.api.ITypeRefProvider;
import de.starima.pfw.base.processor.description.config.api.ITypeResolutionContext;
import lombok.*;
import lombok.experimental.Accessors;

import java.lang.reflect.Type;

@Getter
@Setter
@Accessors(chain = true) // Behalten wir fÃ¼r die flÃ¼ssige API bei
@ToString(callSuper = true) // WICHTIG: Bezieht die toString()-Methode der Elternklasse mit ein
@EqualsAndHashCode(callSuper = true) // WICHTIG: Bezieht die Felder der Elternklasse in equals() und hashCode() mit ein
@NoArgsConstructor
public class DefaultTypeResolutionContext extends DefaultTaskContext implements ITypeResolutionContext {
    private Type typeToResolve;
    private ITypeRefProvider rootProvider;

    /**
     * Erzeugt einen neuen Kind-Kontext fÃ¼r eine rekursive Typ-AuflÃ¶sungsaufgabe.
     *
     * @param parent Der Eltern-Kontext, von dem dieser Kontext abstammt.
     * @param newTypeToResolve Der neue, spezifische Typ, der in dieser Sub-Aufgabe aufgelÃ¶st werden soll.
     */
    public DefaultTypeResolutionContext(ITypeResolutionContext parent, Type newTypeToResolve) {
        // Setzt die Eltern-Kind-Beziehung und erbt den runtimeContext
        super(parent);

        // Erbt den Root-Provider, da dieser fÃ¼r die gesamte Operation gleich bleibt
        this.rootProvider = parent.getRootProvider();

        // Setzt die neue, spezifische Aufgabe fÃ¼r diesen Kontext
        this.typeToResolve = newTypeToResolve;
    }
}