package de.starima.pfw.base.util;

import java.util.UUID;

import de.starima.pfw.base.util.api.IUUIDIdentifier;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter
@EqualsAndHashCode @ToString
public class UUIDIdentifier implements IUUIDIdentifier {
    private UUID uuid;
}