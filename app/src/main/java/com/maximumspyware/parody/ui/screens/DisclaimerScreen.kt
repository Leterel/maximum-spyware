package com.maximumspyware.parody.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maximumspyware.parody.ui.theme.*

@Composable
fun DisclaimerScreen(onAccept: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Ink).safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("MS / 002", color = Lime, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            Text("100% FAKE", color = Lime, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text("DEINE\nPRIVATSPHÄRE\nHAT FEIERABEND.", color = Paper,
            fontSize = 36.sp, lineHeight = 38.sp, fontWeight = FontWeight.Black)
        Text("MAXIMUM SPYWARE", color = Lime, fontSize = 15.sp,
            letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
        Radar(progress = 0f, running = false, modifier = Modifier.fillMaxWidth().height(180.dp))
        Column(Modifier.fillMaxWidth().border(1.dp, Border, RoundedCornerShape(16.dp))
            .background(Panel, RoundedCornerShape(16.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Alles Show. Wirklich alles.", color = Lime, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Diese Spaß-App simuliert Überwachung. Jeder Fund ist ausgedacht. " +
                "Sie liest keine persönlichen Daten und greift weder auf Kamera noch Mikrofon oder Standort zu.",
                color = Paper, fontSize = 16.sp, lineHeight = 24.sp)
            Text("KEIN INTERNET · KEINE TRACKER · KEINE BERECHTIGUNGEN", color = Muted,
                fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 17.sp)
        }
        Button(onClick = onAccept, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
            shape = RoundedCornerShape(10.dp)) {
            Text("OKAY, SPIONIER MICH FAKE AUS →", fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
        Text("NULL DATEN. MAXIMALES THEATER.", color = Muted,
            fontFamily = FontFamily.Monospace, fontSize = 11.sp)
    }
}
