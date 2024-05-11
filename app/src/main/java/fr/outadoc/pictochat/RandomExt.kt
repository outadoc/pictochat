package fr.outadoc.pictochat

import java.security.SecureRandom

private val random = SecureRandom()

fun randomInt(): Int {
    return random.nextInt()
}
