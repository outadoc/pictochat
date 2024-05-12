package fr.outadoc.pictochat.ui.room

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun RoomInputTextField(
    modifier: Modifier = Modifier,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
) {
    BasicTextField(
        modifier = modifier.size(1.dp),
        value = value,
        onValueChange = onValueChange
    )
}
