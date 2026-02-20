package de.starima.pfw.base.processor.description.config.api;

import javax.annotation.PropertyKey;
import java.util.List;

/**
 * Beschreibt die Konfiguration eines Wertes. Dies ist das HerzstÃ¼ck der
 * rekursiven Konfigurationsstruktur.
 */
public interface IValueConfig extends IDescriptorConfig {

    /**
     * Definiert die erwarteten hierarchischen Kategorien fÃ¼r einen Prozessor oder ein Wertobjekt,
     * das diesem Parameter zugewiesen wird. Ein zugewiesenes Objekt muss in mindestens einer
     * der hier definierten Kategorien sein. Das Matching ist hierarchisch
     * (z.B. "Transformator" matcht "Transformator/DeploymentArtifact").
     */
    List<String> getRequiredCategories();
    void setRequiredCategories(List<String> requiredCategories);

    /**
     * Definiert die erwarteten hierarchischen Sub-Kategorien.
     */
    List<String> getRequiredSubCategories();
    void setRequiredSubCategories(List<String> requiredSubCategories);

    /**
     * Definiert eine Liste von Tags, die ein Prozessor oder Wertobjekt haben muss,
     * um fÃ¼r diesen Parameter gÃ¼ltig zu sein. Ein zugewiesenes Objekt muss alle
     * hier definierten Tags besitzen (UND-VerknÃ¼pfung).
     */
    List<String> getRequiredTags();
    void setRequiredTags(List<String> requiredTags);

    /**
     * Die reichhaltige, unverÃ¤nderliche ReprÃ¤sentation des Java-Typs.
     * Wird benutzt, um eine geeignete ValueFunction zu finden, falls diese nicht explizit
     * Ã¼ber die functionConfig definiert wurde.
     */
    ITypeRef getTypeRef();
    void setTypeRef(ITypeRef typeRef);

    /**
     * Gibt den erwarteten Zieltyp fÃ¼r die Transformation an.
     * Dies wird fÃ¼r komplexe Typen verwendet, die zur Laufzeit eine
     * spezielle Behandlung benÃ¶tigen.
     * - Fnums: Die Enum-Klasse selbst (z.B. ProcessorScope.class)
     * - FÃ¼r Klassen: Die Klasse selbst (z.B. MyCustomClass.class)
     */
    Class<?> getTargetType();
    void setTargetType(Class<?> targetType);

    /**
     * Die Konfiguration fÃ¼r die zugehÃ¶rige IValueFunction.
     * Wenn diese Konfiguration gesetzt ist, hat sie Vorrang vor der automatischen
     * Suche einer ValueFunction basierend auf der TypeRef.
     */
    IValueFunctionConfig getFunctionConfig();
    void setFunctionConfig(IValueFunctionConfig functionConfig);
}