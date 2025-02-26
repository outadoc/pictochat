package fr.outadoc.pictochat.domain

enum class ProfileColor(val id: Int) {
    Color1(1),
    Color2(2),
    Color3(3),
    Color4(4),
    Color5(5),
    Color6(6),
    Color7(7),
    Color8(8),
    Color9(9),
    Color10(10),
    Color11(11),
    Color12(12),
    Color13(13),
    Color14(14),
    Color15(15),
    Color16(16);

    companion object {
        val Default = Color1

        fun fromId(id: Int): ProfileColor {
            return entries.first { it.id == id }
        }
    }
}
