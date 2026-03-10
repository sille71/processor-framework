package de.starima.pfw.base.processor.context.domain;

import de.starima.pfw.base.processor.context.api.IProposalContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Standardimplementierung von {@link IProposalContext}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DefaultProposalContext extends DefaultTaskContext implements IProposalContext {
    private String filterText;
}