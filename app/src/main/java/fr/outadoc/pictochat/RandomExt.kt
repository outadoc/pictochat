package fr.outadoc.pictochat

import java.security.SecureRandom

fun randomInt(): Int {
    return SecureRandom.getInstanceStrong().nextInt()
}
