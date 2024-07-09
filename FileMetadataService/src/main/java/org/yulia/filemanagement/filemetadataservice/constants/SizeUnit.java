package org.yulia.filemanagement.filemetadataservice.constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Objects;

/**
 * Represents the different units of size measurement for files.
 */
public enum SizeUnit {
    bytes, kb, mb, gb;

    private static final Logger logger = LoggerFactory.getLogger(SizeUnit.class);

    /**
     * Converts a string representation of a size unit to a SizeUnit enum.
     * If the input is null, the default unit is used.
     *
     * @param unit the size unit as a string, may be null.
     * @param defaultUnit the default size unit as a string, used if 'unit' is null.
     * @return the corresponding SizeUnit enum object.
     * @throws IllegalArgumentException if the unit is not valid.
     */
    public static SizeUnit fromString(String unit, String defaultUnit) {
        if (Objects.isNull(unit)) {
            logger.debug("Unit is null, defaulting to: {}", defaultUnit);
            unit = defaultUnit;
        }

        try {
            SizeUnit result = SizeUnit.valueOf(unit.toLowerCase());
            logger.debug("Parsed unit: {}", result);
            return result;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid size unit: {}. Valid units are: bytes, kb, mb, gb.", unit);
            throw new IllegalArgumentException("Invalid size unit: " + unit + ". Valid units are: bytes, kb, mb, gb.");
        }
    }
}
