package fr.outadoc.pictochat.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.sp
import fr.outadoc.pictochat.R

@OptIn(ExperimentalTextApi::class)
val PictoChatFont =
    FontFamily(
        Font(
            R.font.pictochat,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(950),
                FontVariation.width(30f),
                FontVariation.slant(-6f),
            )
        )
    )

val PictoChatTextStyle = TextStyle(
    fontFamily = PictoChatFont,
    fontSize = 8.sp
)
