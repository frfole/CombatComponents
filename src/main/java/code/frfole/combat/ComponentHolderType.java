package code.frfole.combat;

/**
 * Types of components holders.
 */
public enum ComponentHolderType {
    /**
     * Holder is nobody
     */
    NONE,
    /**
     * Holder is entity
     */
    ENTITY,
    /**
     * Holder is entity's item in main hand
     */
    ITEM_IN_MAIN_HAND,
    /**
     * Holder is entity's item in off hand
     */
    ITEM_IN_OFF_HAND;

    /**
     * Gets the holder corresponding to the given ID.
     * @param integer the ID
     * @return the holder detonated by the ID
     */
    public static ComponentHolderType fromInteger(Integer integer) {
        if (integer == null) {
            return NONE;
        }
        return switch (integer) {
            case 1 -> ENTITY;
            case 2 -> ITEM_IN_MAIN_HAND;
            case 3 -> ITEM_IN_OFF_HAND;
            default -> NONE;
        };
    }
}
