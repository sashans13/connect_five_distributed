package common

import java.lang.Math

object Constants {
    // Bootstrap
    val BOOTSTRAP_IP_ADDRESS: String = "localhost"
    val BOOTSTRAP_LISTENER_PORT: Int = 9001

    // Network params
    val PORT_RANGE_START: Int = 20000
    val PORT_RANGE_WIDTH: Int = 1024 // TODO: Fix this properly (2^...)

    // Chord params
    val CHORD_SIZE_EXPONENT: Int = 10
    val FINGER_TABLE_SIZE: Int = CHORD_SIZE_EXPONENT
    val CHORD_SIZE: Int = Math.pow(2, CHORD_SIZE_EXPONENT).toInt

    // Other
    val MAX_JOIN_ATTEMPTS = 5
    val STABILIZE_SLEEP_TIME_MS: Int = 500
}
