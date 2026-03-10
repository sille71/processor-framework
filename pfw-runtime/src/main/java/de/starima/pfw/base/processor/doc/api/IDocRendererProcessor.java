package de.starima.pfw.base.processor.doc.api;

import de.starima.pfw.base.processor.api.IProcessor;

import java.io.IOException;

public interface IDocRendererProcessor extends IProcessor {
    /**
     * Erzeugt die allgemeine Dokumentation eines Prozessors, wie sie in der Annotation und evtl. Doku Files (MarkDown, AsciiDoc,...) im Source Code hinterlegt ist.
     * @param processor
     * @return
     */
    public String renderGeneralDocumentation(IProcessor processor);

    public String renderHtml(String assetName) throws IOException;
}