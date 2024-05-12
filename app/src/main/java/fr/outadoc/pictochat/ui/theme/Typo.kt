package fr.outadoc.pictochat.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.LineHeightStyle.Alignment
import androidx.compose.ui.text.style.LineHeightStyle.Trim
import androidx.compose.ui.unit.sp
import fr.outadoc.pictochat.R

val PictoChatFont =
    FontFamily(
        Font(R.font.nds)
    )

val PictoChatTextStyle = TextStyle(
    fontFamily = PictoChatFont,
    fontSize = 6.sp,
    lineHeightStyle = LineHeightStyle(
        alignment = Alignment.Proportional,
        trim = Trim.Both
    ),
    lineHeight = 5.sp,
)
