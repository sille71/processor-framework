package de.starima.pfw.base.processor.description.config;

import de.starima.pfw.base.processor.description.config.api.ITypeRef;
import lombok.*;

import java.util.List;


/**
 * Standard-Implementierung von {@link ITypeRef}.
 *
 * Dies ist eine unverÃ¤nderliche Datenklasse (Value Object), die sich wie ein Record verhÃ¤lt.
 * Instanzen sollten ausschlieÃŸlich Ã¼ber den Builder (z.B. via TypeRefProvider) erzeugt werden.
 * Die is...()-Methoden werden von Lombok automatisch fÃ¼r die boolean-Felder generiert.
 *
 * TODO: bei Upgrade auf Java 21 wird das zum record!
 */
@Builder
@Value
public class DefaultTypeRef implements ITypeRef {
    Kind kind;
    String rawTypeName;
    String typeSignature;

    @Singular
    List<ITypeRef> typeArguments;

    // --- Flags, die die Natur des Typs beschreiben ---
    // @Builder.Default sorgt dafÃ¼r, dass der Wert 'false' ist, wenn er nicht explizit gesetzt wird.

    @Builder.Default
    boolean processor = false;

    @Builder.Default
    boolean valueObject = false;

    @Builder.Default
    boolean numeric = false;

    @Builder.Default
    boolean bool = false; // Feldname 'bool', da 'boolean' ein reserviertes Wort ist

    @Builder.Default
    boolean date = false;

    @Builder.Default
    boolean time = false;

    @Builder.Default
    boolean string = false;

    @Builder.Default
    boolean anEnum = false; // Feldname 'anEnum', da 'enum' ein reserviertes Wort ist

    @Builder.Default
    boolean polymorphic = false;

    // Die Getter-Methoden wie isProcessor(), isNumeric() etc. werden von @Value automatisch generiert.
    // FÃ¼r die Felder mit abweichenden Namen implementieren wir die Interface-Methode explizit.

    @Override
    public boolean isBoolean() {
        return this.bool;
    }

    @Override
    public boolean isEnum() {
        return this.anEnum;
    }
}