package net.maxsmr.core.ui.compose

import android.graphics.Typeface
import android.text.Spanned
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.compose.foundation.clickable
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

const val URL_TAG = "url"

/**
 * @param addStyleFunc лямбда для кастомной обработки имеющегося [CharacterStyle],
 * false - если обработка не была произведена
 */
fun Spanned.toAnnotateString(
    baseSpanStyle: SpanStyle? = null,
    linkColor: Color = Color.Unspecified,
    addStyleFunc: (CharacterStyle) -> Boolean = { false },
): AnnotatedString {
    return buildAnnotatedString {
        val spanned = this@toAnnotateString
        append(spanned.toString())
        baseSpanStyle?.let { addStyle(it, 0, length) }
        getSpans(0, spanned.length, CharacterStyle::class.java).forEach { span ->
            val start = getSpanStart(span)
            val end = getSpanEnd(span)
            if (!addStyleFunc(span)) {
                when (span) {
                    is StyleSpan -> when (span.style) {
                        Typeface.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                        Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                        Typeface.BOLD_ITALIC -> addStyle(
                            SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                            start,
                            end
                        )
                    }

                    is UnderlineSpan -> addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
                    is ForegroundColorSpan -> addStyle(SpanStyle(color = Color(span.foregroundColor)), start, end)
                    is URLSpan -> {
                        addStyle(
                            SpanStyle(
                                textDecoration = TextDecoration.Underline,
                                color = linkColor
                            ), start, end
                        )
                        addStringAnnotation(URL_TAG, span.url, start, end)
                    }
                }
            }
        }
    }
}

@Composable
fun UrlText(
    text: Spanned,
    modifier: Modifier = Modifier,
    baseSpanStyle: SpanStyle? = null,
    isHighlightLink: Boolean = false,
    style: TextStyle = LocalTextStyle.current,
    onUrlClick: ((url: String) -> Unit)? = null,
) {
    val uriHandler = LocalUriHandler.current
    val linkColor = if (isHighlightLink) Color.Blue else Color.Unspecified
    val annotatedString = text.toAnnotateString(baseSpanStyle = baseSpanStyle, linkColor = linkColor)
    Text(
        modifier = modifier.clickable {
            annotatedString.getStringAnnotations(URL_TAG, 0, annotatedString.length).firstOrNull()?.let {
                onUrlClick?.let { click -> click(it.item) } ?: uriHandler.openUri(it.item)
            }
        },
        text = annotatedString,
        style = style,
    )
}