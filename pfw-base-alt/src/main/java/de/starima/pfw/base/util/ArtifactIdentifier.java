package de.starima.pfw.base.util;

import de.starima.pfw.base.exception.InvalidVersionNumberException;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor @AllArgsConstructor
@Getter @Setter
@EqualsAndHashCode
public class ArtifactIdentifier {
    private String artifactId;
	private Version version;

	public ArtifactIdentifier(String version) throws InvalidVersionNumberException {
		this(version.contains(":") ? version.split(":")[0] : "",
			version.contains(":") ? version.split(":")[1] : version);
	}

    public ArtifactIdentifier(final String artifactId, final String version) throws InvalidVersionNumberException {
        this.artifactId = artifactId.trim();
        this.version = new Version(version);
    }

    public String getAsLabel() {
        if (artifactId != null) {
			if (version != null) 
				return artifactId.concat("-" + version.toString());
			return artifactId;
		}
		return "-1";
    }

	public static String getAsString(String artifactId, String version) {
		if (artifactId != null) {
			String id = artifactId.trim();
			if (version != null) id = id.concat(":" + version);
			return id;
		}
		return "";
	}

	@Override
	public String toString() {
		return getAsString(artifactId, version != null ? version.toString() : null);
	}
}