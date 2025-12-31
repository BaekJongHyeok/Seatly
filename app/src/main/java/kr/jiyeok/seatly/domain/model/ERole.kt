package kr.jiyeok.seatly.domain.model

/**
 * Enum representing user roles in the system.
 */
enum class ERole {
    USER,
    ADMIN;

    companion object {
        /**
         * Converts a string to ERole, defaulting to USER if not recognized.
         */
        fun fromString(role: String): ERole {
            return try {
                valueOf(role.uppercase())
            } catch (e: IllegalArgumentException) {
                USER
            }
        }

        /**
         * Determines if a list of role strings contains ADMIN.
         */
        fun isAdmin(roles: List<String>): Boolean {
            return roles.any { it.equals("ADMIN", ignoreCase = true) }
        }
    }
}
