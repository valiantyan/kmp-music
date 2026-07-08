package com.yanhao.kmpmusic.feature.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = searchTopBarHeight)
            .background(color = searchTopBarColor)
            .padding(horizontal = searchHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchRoundIconButton(
            contentDescription = "返回",
            onClick = onBack,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(size = 20.dp),
                tint = searchAccentColor,
            )
        }
        SearchInputPill(
            query = query,
            onQuery = onQuery,
            modifier = Modifier.weight(weight = 1f),
        )
    }
}

// 搜索输入框直接接控制器 query，清空按钮复用同一条搜索状态链路。
@Composable
private fun SearchInputPill(
    query: String,
    onQuery: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var textFieldValue: TextFieldValue by remember {
        mutableStateOf(value = TextFieldValue(text = query))
    }
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
            .padding(start = 18.dp, end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            modifier = Modifier.size(size = 18.dp),
            tint = searchPlaceholderTextColor,
        )
        BasicTextField(
            value = textFieldValue,
            onValueChange = { nextValue: TextFieldValue ->
                textFieldValue = nextValue
                onQuery(nextValue.text)
            },
            modifier = Modifier.weight(weight = 1f),
            textStyle = TextStyle(
                color = searchPrimaryTextColor,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
            ),
            cursorBrush = SolidColor(value = searchAccentColor),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = "搜索歌曲、专辑或艺人",
                            color = searchPlaceholderTextColor,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
            },
        )
        SearchRoundIconButton(
            contentDescription = "清空搜索",
            onClick = { onQuery("") },
            size = 24.dp,
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                modifier = Modifier.size(size = 14.dp),
                tint = searchSecondaryTextColor,
            )
        }
    }
}
