package de.starima.pfw.base.util;

import java.util.Comparator;

public class ArtifactIdentifierComparator implements Comparator<ArtifactIdentifier> {

    /**
     * Compare two artifactIdentifiers containing a version number.
     * @param predecessorArtifactIdentifier - the first artifactIdentifier to compare
     * @param successorArtifactIdentifier - the second artifactIdentifier to compare
     * @returnValue returns -1 if predecessor is less than successor, 0 if predecessor is equal to successor
     *              and 1 if predecessor is greater than successor
     */    
    @Override
    public int compare(ArtifactIdentifier predecessorArtifactIdentifier, ArtifactIdentifier successorArtifactIdentifier) {
        if (predecessorArtifactIdentifier.getVersion().getMajorNumber() > successorArtifactIdentifier.getVersion().getMajorNumber())
            return 1;
        else if (predecessorArtifactIdentifier.getVersion().getMajorNumber() < successorArtifactIdentifier.getVersion().getMajorNumber())
            return -1;
        else {
            if (predecessorArtifactIdentifier.getVersion().getMinorNumber() > successorArtifactIdentifier.getVersion().getMinorNumber())
                return 1;
            else if (predecessorArtifactIdentifier.getVersion().getMinorNumber() < successorArtifactIdentifier.getVersion().getMinorNumber())
                return -1;
            else {
                if (predecessorArtifactIdentifier.getVersion().getPatchLevel() > successorArtifactIdentifier.getVersion().getPatchLevel())
                    return 1;
                else if (predecessorArtifactIdentifier.getVersion().getPatchLevel() < successorArtifactIdentifier.getVersion().getPatchLevel())
                    return -1;
                else {
                    if (predecessorArtifactIdentifier.getVersion().getIsSnapshot() && !successorArtifactIdentifier.getVersion().getIsSnapshot())
                        return -1;
                    else if (!predecessorArtifactIdentifier.getVersion().getIsSnapshot() && successorArtifactIdentifier.getVersion().getIsSnapshot())
                        return 1;
                    else
                        //if (predecessorArtifactIdentifier.getVersion().toString().equals(successorArtifactIdentifier.getVersion().toString()))
                        return 0;   // it must be equal then
                }
            }
        }
    }

    public static Boolean isValidSuccessor(String predecessorVersion, String successorVersion) {
        try {
            ArtifactIdentifier predecessorArtifactIdentifier = new ArtifactIdentifier(predecessorVersion);
            ArtifactIdentifier successorArtifactIdentifier = new ArtifactIdentifier(successorVersion);
            return isValidSuccessor(predecessorArtifactIdentifier, successorArtifactIdentifier);
        }
        catch (Exception e) {
            return false;
        }
    }

    public static Boolean isValidSuccessor(ArtifactIdentifier predecessorArtifactIdentifier, ArtifactIdentifier successorArtifactIdentifier) {
        ArtifactIdentifierComparator comparator = new ArtifactIdentifierComparator();
        Integer compareValue = comparator.compare(predecessorArtifactIdentifier, successorArtifactIdentifier);

        if (compareValue <= 0)
            return true;
        else
            return false;
    }
}