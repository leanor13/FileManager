package org.yulia.filemanagement.filemetadataservice.constants;

import java.util.Objects;

public enum SizeUnit {
    bytes, kb, mb, gb;

    public static SizeUnit fromString(String unit, String defaultUnit) {
        if (Objects.isNull(unit)) {
            unit = defaultUnit;
        }
        try {
            return SizeUnit.valueOf(unit.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid size unit: " + unit + ". Valid units are: bytes, kb, mb, gb.");
        }
    }
}
