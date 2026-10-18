package com.nya.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nya.app.ui.theme.LocalNya
import com.nya.app.viewmodel.Loadable

@Composable
fun NyaButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, loading: Boolean = false) {
    val c = LocalNya.current
    Box(
        modifier.fillMaxWidth().height(54.dp).pressScale(onClick = { if (!loading) onClick() })
            .clip(RoundedCornerShape(27.dp)).background(Brush.verticalGradient(listOf(c.b1, c.b2))),
        contentAlignment = Alignment.Center
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = c.on, strokeWidth = 2.dp)
        else Text(text, color = c.on, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
fun AuthMessage(error: String?, info: String?) {
    val c = LocalNya.current
    if (error != null) {
        Text(error, color = NyaDanger, fontSize = 13.sp,
            modifier = Modifier.padding(top = 12.dp).semantics { liveRegion = LiveRegionMode.Polite })
    } else if (info != null) {
        Text(info, color = c.sb, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            Modifier.size(28.dp).semantics { contentDescription = "Loading" },
            color = LocalNya.current.ac, strokeWidth = 2.dp
        )
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    val c = LocalNya.current
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = c.tx, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Text(body, color = c.sb, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        action?.invoke()
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, color = NyaDanger, fontSize = 14.sp, textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        Text("Try again", color = LocalNya.current.ac, fontSize = 14.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 10.dp).pressScale { onRetry() })
    }
}

/** Shows loading / error in the content area, and [content] once [state] is ready. */
@Composable
fun <T> LoadableContent(state: Loadable<T>, onRetry: () -> Unit, content: @Composable (T) -> Unit) {
    val pad = Modifier.padding(vertical = 40.dp)
    when (state) {
        Loadable.Loading -> LoadingState(pad)
        is Loadable.Failed -> ErrorState(state.message, onRetry, pad)
        is Loadable.Ready -> content(state.value)
    }
}

@Composable
fun NyaOutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalNya.current
    val shape = RoundedCornerShape(27.dp)
    Box(
        modifier.fillMaxWidth().height(54.dp).pressScale(onClick = onClick).clip(shape).background(c.sf)
            .border(1.dp, c.ln, shape),
        contentAlignment = Alignment.Center
    ) { Text(text, color = c.tx, fontWeight = FontWeight.Medium, fontSize = 14.sp) }
}

@Composable
fun NyaField(
    label: String, value: String, onChange: (String) -> Unit, password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text, transformation: VisualTransformation = VisualTransformation.None
) {
    val c = LocalNya.current
    Column {
        Text(label, color = c.sb, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = value, onValueChange = onChange, singleLine = true,
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            visualTransformation = if (password) PasswordVisualTransformation() else transformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.ac, unfocusedBorderColor = c.ln,
                focusedTextColor = c.tx, unfocusedTextColor = c.tx, cursorColor = c.ac,
                focusedContainerColor = c.sf, unfocusedContainerColor = c.sf
            )
        )
    }
}
/** Shows a digits-only string as DD/MM/YYYY; slashes appear only once more digits follow them. */
object DateTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val d = text.text
        val shown = buildString {
            d.forEachIndexed { i, ch ->
                if (i == 2 || i == 4) append('/')
                append(ch)
            }
        }
        return TransformedText(AnnotatedString(shown), object : OffsetMapping {
            override fun originalToTransformed(offset: Int) =
                offset + (if (offset > 2) 1 else 0) + (if (offset > 4) 1 else 0)

            override fun transformedToOriginal(offset: Int) =
                (offset - (if (offset > 2) 1 else 0) - (if (offset > 5) 1 else 0)).coerceIn(0, d.length)
        })
    }
}
