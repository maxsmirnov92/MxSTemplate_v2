package net.maxsmr.designsystem.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.Checkbox
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CheckBoxWithText(
    isChecked: Boolean,
    text: String,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val isCheckedState = remember { mutableStateOf(isChecked) }
    Row(
        modifier = modifier
            .clickable {
                val newValue = !isCheckedState.value
                isCheckedState.value = newValue
                onCheckedChange?.invoke(newValue)
            }
    ) {
        Checkbox(
            checked = isCheckedState.value,
            onCheckedChange = null
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = text,
        )
    }
}

@Composable
fun RadioButtonWithText(
    isSelected: Boolean,
    text: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val isSelectedState = remember { mutableStateOf(isSelected) }
    Row(
        modifier = modifier
            .clickable {
                isSelectedState.value = true
                onClick?.invoke()
            }
    ) {
        RadioButton(
            selected = isSelectedState.value,
            onClick = null
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = text,
        )
    }
}