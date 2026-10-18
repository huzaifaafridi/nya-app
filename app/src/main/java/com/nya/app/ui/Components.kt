package com.nya.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nya.app.ui.theme.LocalNya
import com.nya.app.viewmodel.Loadable

@Composable
fun NyaButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, loading: Boolean = false) {
    val c = LocalNya.current
    Box(
        modifier.fillMaxWidth().heightIn(min = 54.dp).pressScale(onClick = { if (!loading) onClick() })
            .clip(RoundedCornerShape(27.dp)).background(Brush.verticalGradient(listOf(c.b1, c.b2)))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (loading) CircularProgressIndicator(
            Modifier.size(22.dp).semantics { contentDescription = "Loading" }, color = c.on, strokeWidth = 2.dp
        )
        else Text(text, color = c.on, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun AuthMessage(error: String?, info: String?) {
    val c = LocalNya.current
    val line = Modifier.padding(top = 12.dp).semantics { liveRegion = LiveRegionMode.Polite }
    if (error != null) Text(error, color = NyaDanger, fontSize = 13.sp, modifier = line)
    else if (info != null) Text(info, color = c.sb, fontSize = 13.sp, modifier = line)
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
            modifier = Modifier.pressScale { onRetry() }.minimumInteractiveComponentSize())
    }
}

/** Fades between loading / error and [content] once [state] is ready; new values of the same kind update in place. */
@Composable
fun <T> LoadableContent(state: Loadable<T>, onRetry: () -> Unit, content: @Composable (T) -> Unit) {
    val pad = Modifier.padding(vertical = 40.dp)
    val fade = motionSpec<Float>()
    AnimatedContent(
        state, transitionSpec = { (fadeIn(fade) togetherWith fadeOut(fade)).using(null) },
        contentKey = { it::class }, label = "loadable"
    ) { s ->
        when (s) {
            Loadable.Loading -> LoadingState(pad)
            is Loadable.Failed -> ErrorState(s.message, onRetry, pad)
            is Loadable.Ready -> Column { content(s.value) }
        }
    }
}

/** Fills its space and centres [content]; scrolls instead when the content is taller (big fonts, small phones). */
@Composable
fun CenterScroll(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(modifier) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = maxHeight).verticalScroll(rememberScrollState()),
            Arrangement.Center, Alignment.CenterHorizontally, content = content
        )
    }
}

@Composable
fun NyaOutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalNya.current
    val shape = RoundedCornerShape(27.dp)
    Box(
        modifier.fillMaxWidth().heightIn(min = 54.dp).pressScale(onClick = onClick).clip(shape).background(c.sf)
            .border(1.dp, c.ln, shape).padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = c.tx, fontWeight = FontWeight.Medium, fontSize = 14.sp, textAlign = TextAlign.Center) }
}

@Composable
fun NyaField(
    label: String, value: String, onChange: (String) -> Unit, password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text, transformation: VisualTransformation = VisualTransformation.None
) {
    val c = LocalNya.current
    Column(Modifier.semantics(mergeDescendants = true) {}) {
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
