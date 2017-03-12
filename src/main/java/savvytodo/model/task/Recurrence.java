package savvytodo.model.task;

import savvytodo.commons.exceptions.IllegalValueException;

/**
 * @author A0140016B
 *
 * Represents Task's Recurrence in the task manager Guarantees: immutable;
 * is valid as declared in {@link #isValidRecurrence(String, int)} *
 */
public class Recurrence {

    /**
     * @author A0140016B
     *
     * Specifies the type of recurrence for the task
     * Defaults to none if it is a one-time task
     */
    public enum Type {
        None,
        Daily,
        Weekly,
        Monthly,
        Yearly;

        /**
         * Get type enum object from it's name, ignoring cases
         * @param String recurrence type
         * @return Corresponding enum object
         */
        public static Type valueOfIgnoreCase(String retype) {
            for (Type type : Type.values()) {
                if (type.toString().equalsIgnoreCase(retype)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown recurrence type: " + retype);
        }

        /**
         * Compare enum object with String to see if it matches
         * @param String retype cannot be null
         * @return boolean of wheather string matches enum object
         */
        public static boolean matches(String retype) {
            for (Type type : Type.values()) {
                if (type.toString().equalsIgnoreCase(retype)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Compare enum object with String to see if it matches
         * @param String retype cannot be null
         * @return boolean of wheather string matches enum object
         */
        public static boolean isNone(String retype) {
            if (Type.None.toString().equalsIgnoreCase(retype)) {
                return true;
            } else {
                return false;
            }
        }

    }

    public final int occurences;
    public final Type type;

    public static final String MESSAGE_RECURR_CONSTRAINTS =
            "If recurrence type is NONE, occurences can only be 0";
    public static final String MESSAGE_RECURR_NOT_MATCH =
            "Task recurrence type should be 'none', 'daily', 'weekly', 'monthly' or 'yearly'";

    /**
     * Validates given Recurrence.
     * @throws IllegalValueException if given Recurrence is invalid.
     */
    public Recurrence(String type, int occurences) throws IllegalValueException {
        assert type != null;
        assert occurences >= 0;
        String trimmedType = type.trim();
        if (!Type.matches(type)) {
            throw new IllegalValueException(MESSAGE_RECURR_NOT_MATCH);
        }
        if (!isValidRecurrence(trimmedType, occurences)) {
            throw new IllegalValueException(MESSAGE_RECURR_CONSTRAINTS);
        }
        this.type = Type.valueOfIgnoreCase(trimmedType);
        this.occurences = occurences;
    }

    /**
     * Returns true if a given string is a valid task Recurrence and num is more than -1.
     */
    public static boolean isValidRecurrence(String type, int numOfTimes) {
        if (numOfTimes == 0 && Type.isNone(type) == false) {
            return true;
        } else {
            return false;
        }
    }

}
