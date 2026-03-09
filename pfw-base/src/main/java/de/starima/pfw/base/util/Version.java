package de.starima.pfw.base.util;

import java.util.StringTokenizer;

import de.starima.pfw.base.exception.InvalidVersionNumberException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor @AllArgsConstructor @Builder(toBuilder = true)
@Getter @Setter
public class Version {
    private Long id;
	private Integer majorNumber;
	private Integer minorNumber;
	private Integer patchLevel;
	private Integer buildNumber;
    private Boolean isSnapshot;
	private String shortDescription;
	private String longDescription;

    public Version(final String versionNumber) throws InvalidVersionNumberException {
		this.setVersionNumber(versionNumber);
	}

	public String toString() {
		return getVersionNumberAsString();
	}

	private String getVersionNumberAsString() {
		return this.majorNumber + "." + this.minorNumber + "." + this.patchLevel + (this.isSnapshot != null && this.isSnapshot ? "-SNAPSHOT" : "");
	}

	/**
	 * Sets the version numbers (major, minor, patchlevel) according a string representation.
	 * Marks snapshot in case the specified version string ends with snapshot.
	 * @param versionNumber
	 * @throws InvalidVersionNumberException 
	 */
	public void setVersionNumber(String versionNumber) throws InvalidVersionNumberException {
		if (versionNumber == null || versionNumber.isEmpty())
			throw new InvalidVersionNumberException("Version string is null or empty.");

		versionNumber = versionNumber.trim();

		String versionNumberTemp = versionNumber;

		if (versionNumber.toLowerCase().endsWith("snapshot")) {
			setIsSnapshot(true);
			versionNumberTemp = versionNumber.toLowerCase().subSequence(0, versionNumber.toLowerCase().indexOf("snapshot") - 1).toString();
		} else
			setIsSnapshot(false);

		StringTokenizer tokenizer = new StringTokenizer(versionNumberTemp, ".");
		try {
			setMajorNumber(Integer.parseInt(tokenizer.nextToken()));
			setMinorNumber(Integer.parseInt(tokenizer.nextToken()));
			setPatchLevel(Integer.parseInt(tokenizer.nextToken()));
			
		} catch (Exception e) {
			throw new InvalidVersionNumberException("Version string \"" + versionNumber + "\" is invalid! Use Format <number>.<number>.<number>!");
		}
	}
}