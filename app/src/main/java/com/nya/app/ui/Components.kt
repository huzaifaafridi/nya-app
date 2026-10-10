package com.nya.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nya.app.ui.theme.LocalNya

@Composable
fun NyaButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalNya.current
    Box(
        modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp))
            .background(Brush.verticalGradient(listOf(c.b1, c.b2))).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = c.on, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
}

@Composable
fun NyaOutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalNya.current
    val shape = RoundedCornerShape(27.dp)
    Box(
        modifier.fillMaxWidth().height(54.dp).clip(shape).background(c.sf)
            .border(1.dp, c.ln, shape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = c.tx, fontWeight = FontWeight.Medium, fontSize = 14.sp) }
}

@Composable
fun NyaField(label: String, value: String, onChange: (String) -> Unit, password: Boolean = false) {
    val c = LocalNya.current
    Column {
        Text(label, color = c.sb, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = value, onValueChange = onChange, singleLine = true,
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.ac, unfocusedBorderColor = c.ln,
                focusedTextColor = c.tx, unfocusedTextColor = c.tx, cursorColor = c.ac,
                focusedContainerColor = c.sf, unfocusedContainerColor = c.sf
            )
        )
    }
}