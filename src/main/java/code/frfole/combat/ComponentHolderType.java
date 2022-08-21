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
     * Holder is item
     */
    ITEM;

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
            case 2 -> ITEM;
            default -> NONE;
        };
    }
}
