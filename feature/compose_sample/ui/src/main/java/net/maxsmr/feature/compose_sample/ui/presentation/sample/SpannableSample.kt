package net.maxsmr.feature.compose_sample.ui.presentation.sample

import android.text.Spanned
import android.text.style.UnderlineSpan
import android.text.util.Linkify
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.text.toSpannable
import net.maxsmr.commonutils.RangeSpanInfo
import net.maxsmr.commonutils.SubstringSpanInfo
import net.maxsmr.commonutils.createSpanText
import net.maxsmr.commonutils.parseHtmlToSpannedString
import net.maxsmr.core.ui.compose.UrlText
import net.maxsmr.core.ui.compose.toAnnotateString

@Preview
@Composable
fun HtmlUrlTextTest() {
    val htmlText =
        "<b>Для просмотра нового о compose</b> \n<a href=\"https://developer.android.com/jetpack/androidx/releases/compose\" target=\"_blank\">" +
                "нажмите на этот текст</a>, там много интересного"
    val spannedText = htmlText.parseHtmlToSpannedString() ?: return
    UrlText(spannedText)
}

@Preview
@Composable
fun UrlTextTest() {
    val someText = "Хочу открыть https://developer.android.com, что нового?"
    val spannedText = someText.toSpannable() // превращаем в Spannable, так как Linkify работает со Spannable
    Linkify.addLinks(spannedText, Linkify.WEB_URLS) // Ищем и размечаем URLSpan
    UrlText(spannedText)
}

@Preview
@Composable
fun SpannableTextTest() {
    val someText1 = "123534563y4598y34598y3489y589347598734598734589635135"
        .createSpanText(RangeSpanInfo(1, 2, UnderlineSpan())) as Spanned
    val someText2 = "1234534635635135"
        .createSpanText(SubstringSpanInfo.FirstEntry("5135", UnderlineSpan())) as Spanned
    Column(verticalArrangement = Arrangement.SpaceBetween) {
        Text(someText1.toAnnotateString())
        Text(someText2.toAnnotateString())
    }
}
