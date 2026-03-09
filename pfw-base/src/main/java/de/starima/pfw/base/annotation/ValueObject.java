package de.starima.pfw.base.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Markiert eine Klasse als ein konfigurierbares Wertobjekt, das vom Framework
 * beschrieben und in Deskriptoren dargestellt werden kann.
 * Ã„hnlich wie @Processor, aber fÃ¼r Nicht-Prozessor-Klassen.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ValueObject {
    String description() default "";
    String descriptionAssetName() default "";
    String descriptionAssetMimetype() default "";
    String assetProviderIdentifier() default "documentAssetProviderProcessor";

    String[] categories() default {};
    String[] subCategories() default {};
    String[] tags() default {};

    /**
     * der zustÃ¤ndige Descriptor kann explizit angegeben werden.
     * @return
     */
    public String descriptorPrototypeIdentifier() default "defaultStructureValueDescriptor";

    /**
     * Liste von parameterName|value Paaren. Damit kann der value() in der ProcessorParameter Annotation Ã¼berschrieben werden.
     * Das macht in Subklassen Sinn, die den default Wert der Super Klasse Ã¼berschreiben sollen.
     * Zu beachten ist, das hier der Parametername angegeben wird, wie er in der ProcessorParameter Annotation des Feldes definiert ist (in der Regel wird dies der Propertyname sein)
     * @return
     */
    public String[] defaultValues() default {};
}