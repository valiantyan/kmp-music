package com.yanhao.kmpmusic.feature.desktop.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicColors
import com.yanhao.kmpmusic.feature.desktop.DesktopMusicType

/**
 * 桌面输入框统一复用浅色玻璃样式，搜索页可显式开启清除动作，避免影响登录等普通表单。
 */
@Composable
fun DesktopTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    isClearEnabled: Boolean = false,
    onSubmit: () -> Unit = {},
) {
    val interactionSource: MutableInteractionSource = MutableInteractionSource()
    var textFieldValue: TextFieldValue by remember {
        mutableStateOf(value = TextFieldValue(text = value))
    }
    LaunchedEffect(value, textFieldValue.text) {
        if (textFieldValue.text != value) {
            textFieldValue =
                TextFieldValue(
                    text = value,
                    selection = TextRange(index = value.length),
                )
        }
    }
    BasicTextField(
        value = textFieldValue,
        onValueChange = { nextValue: TextFieldValue ->
            textFieldValue = nextValue
            onValueChange(nextValue.text)
        },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        textStyle =
            androidx.compose.ui.text.TextStyle(
                color = DesktopMusicColors.Ink,
                fontSize = DesktopMusicType.Eyebrow,
                fontWeight = FontWeight.SemiBold,
            ),
        interactionSource = interactionSource,
        decorationBox = { innerTextField: @Composable () -> Unit ->
            val shouldShowClearAction: Boolean =
                shouldShowDesktopTextInputClearAction(
                    value = textFieldValue.text,
                    isClearEnabled = isClearEnabled,
                )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.84f),
                border = BorderStroke(width = 1.dp, color = DesktopMusicColors.Line),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = DesktopMusicColors.Muted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (value.isBlank()) {
                            Text(
                                text = placeholder,
                                color = DesktopMusicColors.Muted,
                                fontSize = DesktopMusicType.Eyebrow,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        innerTextField()
                    }
                    DesktopTextInputClearAction(
                        isClearEnabled = isClearEnabled,
                        shouldShowClearAction = shouldShowClearAction,
                        onClear = {
                            textFieldValue = TextFieldValue(text = "")
                            onValueChange("")
                        },
                    )
                }
            }
        },
    )
}

/**
 * 桌面分段控件统一使用浅色胶囊样式，避免各页自行实现导致交互不一致。
 */
@Composable
fun DesktopSegmentedControl(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.72f))
                .border(width = 1.dp, color = Color(0xFFD4DDE3), shape = RoundedCornerShape(10.dp))
                .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEachIndexed { index: Int, label: String ->
            Surface(
                modifier = Modifier.height(30.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (index == selectedIndex) DesktopMusicColors.AccentSoft else Color.Transparent,
                onClick = { onSelect(index) },
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        color = if (index == selectedIndex) DesktopMusicColors.AccentDeep else Color(0xFF303A46),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
