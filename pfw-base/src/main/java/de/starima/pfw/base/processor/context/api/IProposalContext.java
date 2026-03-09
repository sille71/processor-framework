package de.starima.pfw.base.processor.context.api;

/**
 * Definiert den Kontext fÃ¼r eine Anfrage nach mÃ¶glichen Werten (Proposals)
 * fÃ¼r einen Parameter. Dies ermÃ¶glicht es, kontextabhÃ¤ngige und gefilterte
 * Vorschlagslisten zu generieren.
 */
public interface IProposalContext extends ITaskContext {
    /**
     * Liefert einen optionalen Filtertext, der von der UI kommen kann
     * (z.B. bei einer Type-Ahead-Suche).
     *
     * @return Der Filtertext oder null, wenn kein Filter angewendet wird.
     */
    String getFilterText();
}