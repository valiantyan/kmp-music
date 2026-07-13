package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 顶部搜索栏固定在页面顶部，符合二级页无底部 Tab 的独立搜索体验。
@Composable
internal fun SearchTopBar(
    query: String,
    onBack: () -> Unit,
    onQuery: (String) -> Unit,
    onCommitSearch: () -> Unit,
    onInputFocusChanged: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = searchTopBarHeight)
            .background(color = searchTopBarColor)
            .padding(
                start = searchTopBarStartPadding,
                end = searchTopBarEndPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchToolbarBackButton(
            onClick = onBack,
        )
        SearchInputPill(
            query = query,
            onQuery = onQuery,
            onCommitSearch = onCommitSearch,
            onInputFocusChanged = onInputFocusChanged,
            modifier = Modifier.weight(weight = 1f),
        )
    }
}

// 返回槽使用 Figma 48dp 稳定宽度，避免输入框宽度随图标状态跳动。
@Composable
private fun SearchToolbarBackButton(
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(width = searchTopBarBackSlotWidth)
            .height(height = searchTopBarHeight)
            .semantics { this.contentDescription = "返回" }
            .clickable(
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = null,
            modifier = Modifier.size(size = 24.dp),
            tint = searchToolbarContentColor,
        )
    }
}

// 搜索输入框直接接控制器 query，框内提交按钮复用同一条搜索状态链路。
@Composable
private fun SearchInputPill(
    query: String,
    onQuery: (String) -> Unit,
    onCommitSearch: () -> Unit,
    onInputFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var textFieldValue: TextFieldValue by remember {
        mutableStateOf(value = TextFieldValue(text = query))
    }
    var isInputFocused: Boolean by remember { mutableStateOf(value = false) }
    LaunchedEffect(query, textFieldValue.text) {
        if (textFieldValue.text != query) {
            textFieldValue = TextFieldValue(
                text = query,
                selection = TextRange(index = query.length),
            )
        }
    }
    Row(
        modifier = modifier
            .height(height = searchInputHeight)
            .clip(shape = CircleShape)
            .background(color = searchInputColor)
            .padding(
                horizontal = searchInputHorizontalPadding,
                vertical = searchInputVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchInputLeadingIcon()
        BasicTextField(
            value = textFieldValue,
            onValueChange = { nextValue: TextFieldValue ->
                textFieldValue = nextValue
                onQuery(nextValue.text)
            },
            modifier = Modifier
                .weight(weight = 1f)
                .onFocusChanged { focusState: FocusState ->
                    isInputFocused = focusState.isFocused
                    onInputFocusChanged(focusState.isFocused)
                },
            textStyle = TextStyle(
                color = searchPrimaryTextColor,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal,
            ),
            cursorBrush = SolidColor(value = searchToolbarAccentColor),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onCommitSearch() }),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (textFieldValue.text.isEmpty()) {
                        Text(
                            text = "搜索...",
                            modifier = Modifier.padding(
                                start = if (isInputFocused) 3.dp else 0.dp,
                            ),
                            color = searchPlaceholderTextColor,
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
            },
        )
        SearchInputTrailingAction(
            hasQuery = textFieldValue.text.isNotEmpty(),
            onClear = {
                textFieldValue = TextFieldValue(text = "")
                onQuery("")
            },
        )
        SearchInputDivider()
        SearchSubmitButton(onClick = onCommitSearch)
    }
}

// 搜索图标槽按 Figma 保留 36dp 点击前导空间，图标本身保持 20dp。
@Composable
private fun SearchInputLeadingIcon() {
    Box(
        modifier = Modifier.size(size = searchInputIconSlotSize),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            modifier = Modifier.size(size = searchInputIconSize),
            tint = searchPlaceholderTextColor,
        )
    }
}

// 有输入内容时显示清除按钮；空输入保留同宽槽位，避免右侧搜索按钮跳动。
@Composable
private fun SearchInputTrailingAction(
    hasQuery: Boolean,
    onClear: () -> Unit,
) {
    if (!hasQuery) {
        Spacer(modifier = Modifier.size(size = searchInputMicSlotSize))
        return
    }
    Box(
        modifier = Modifier.size(size = searchInputMicSlotSize),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size = searchClearButtonSize)
                .clip(shape = CircleShape)
                .background(color = searchClearButtonColor)
                .semantics { this.contentDescription = "清除输入内容" }
                .clickable(
                    role = Role.Button,
                    onClick = onClear,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                modifier = Modifier.size(size = searchClearIconSize),
                tint = searchClearIconColor,
            )
        }
    }
}

// 搜索框内分隔线保留 4dp 左右呼吸空间，匹配 COUI 搜索框结构。
@Composable
private fun SearchInputDivider() {
    Box(
        modifier = Modifier
            .width(width = searchInputDividerSlotWidth)
            .height(height = searchInputIconSlotSize),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(width = 1.dp)
                .height(height = searchInputDividerHeight)
                .background(color = searchToolbarDividerColor),
        )
    }
}

// 提交按钮对应 Figma 右侧“搜索”文字按钮，颜色按用户要求切换为 App 绿色。
@Composable
private fun SearchSubmitButton(
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(width = searchSubmitButtonWidth)
            .height(height = searchSubmitButtonHeight)
            .clip(shape = CircleShape)
            .clickable(
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "搜索",
            color = searchToolbarAccentColor,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
