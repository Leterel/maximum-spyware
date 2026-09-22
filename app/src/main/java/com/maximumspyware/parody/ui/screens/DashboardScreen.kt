package com.maximumspyware.parody.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.maximumspyware.parody.ScanScript
import com.maximumspyware.parody.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DashboardScreen() {
    var elapsedMs by rememberSaveable { mutableIntStateOf(0) }
    var paused by rememberSaveable { mutableStateOf(false) }
    var report by rememberSaveable { mutableStateOf(false) }
    var scanNumber by rememberSaveable { mutableIntStateOf(1) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val done = ScanScript.complete(elapsedMs)
    val progress = ScanScript.progress(elapsedMs)
    val running = !paused && !done

    LaunchedEffect(lifecycle, paused, scanNumber) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (!paused && !ScanScript.complete(elapsedMs)) {
                delay(ScanScript.TICK_MS.toLong())
                elapsedMs = (elapsedMs + ScanScript.TICK_MS).coerceAtMost(ScanScript.DURATION_MS)
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Ink).safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("MAXIMUM", color = Paper, fontWeight = FontWeight.Black, fontSize = 19.sp, letterSpacing = 2.sp)
                Text("SPYWARE", color = Lime, fontWeight = FontWeight.Black, fontSize = 19.sp, letterSpacing = 2.sp)
            }
            Text("● SIMULATION", color = Lime, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier.border(1.dp, Border, RoundedCornerShape(50)).padding(10.dp))
        }
        HorizontalDivider(color = Border)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("LOKALES THEATER / SITZUNG ${scanNumber.toString().padStart(3, '0')}",
                color = Muted, fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 1.sp)
            if (report) {
                Report()
            } else {
                Text(if (paused && !done) "SPIONAGE MACHT PAUSE." else ScanScript.stage(elapsedMs),
                    color = Paper, fontSize = 29.sp, lineHeight = 33.sp, fontWeight = FontWeight.Black)
                Box(Modifier.fillMaxWidth().border(1.dp, Border, RoundedCornerShape(18.dp))
                    .background(Panel, RoundedCornerShape(18.dp))) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Radar(progress, running, Modifier.fillMaxWidth().height(220.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom) {
                            Column(Modifier.weight(1f)) {
                                Text(if (done) "FAKE-SCAN ABGESCHLOSSEN" else "IMAGINÄRER TIEFENSCAN",
                                    color = Muted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                                Text(if (done) "Niemand wurde ausspioniert." else "Bitte sehr verdächtig aussehen.",
                                    color = Paper, fontSize = 12.sp)
                            }
                            Text("${(progress * 100).toInt()}%", color = Lime, fontSize = 26.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.height(14.dp))
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(3.dp),
                            color = Lime, trackColor = Border)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Metric("ECHTE DATEN", "0", Modifier.weight(1f))
                    Metric("UPLOAD", "0 B", Modifier.weight(1f))
                    Metric("DRAMA", "${(progress * 999).toInt()}%", Modifier.weight(1f))
                }
                Terminal(ScanScript.visibleEntries(elapsedMs))
                if (done) {
                    Button(onClick = { report = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        shape = RoundedCornerShape(10.dp)) { Text("MEINEN FAKE-BERICHT ANSEHEN →", fontWeight = FontWeight.Bold) }
                }
                Text("Alle Meldungen sind erfunden. Es werden keine Daten gelesen oder übertragen.",
                    color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
        HorizontalDivider(color = Border)
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { report = !report }, modifier = Modifier.weight(1f)) {
                Text(if (report) "← SCANNER" else "FAKE-BERICHT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            if (!done) {
                TextButton(onClick = { paused = !paused }, modifier = Modifier.weight(1f)) {
                    Text(if (paused) "WEITER" else "PAUSE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            OutlinedButton(onClick = {
                elapsedMs = 0; paused = false; report = false; scanNumber++
            }, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                Text("NEUER SCAN", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier) {
    Column(modifier.background(Panel, RoundedCornerShape(10.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = Muted, fontFamily = FontFamily.Monospace, fontSize = 8.sp)
        Text(value, color = Lime, fontWeight = FontWeight.Bold, fontSize = 23.sp)
    }
}

@Composable
private fun Terminal(entries: List<ScanScript.Entry>) {
    Column(Modifier.fillMaxWidth().border(1.dp, Border, RoundedCornerShape(10.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("●  ●  ●", color = Muted, fontSize = 10.sp)
            Text("fake://terminal", color = Muted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        }
        HorizontalDivider(color = Border)
        entries.takeLast(5).forEachIndexed { index, entry ->
            Text("[${(entry.atMs / 1000).toString().padStart(2, '0')}] > ${entry.message}",
                color = if (index == entries.takeLast(5).lastIndex) Lime else Muted,
                fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun Report() {
    Text("WIR HABEN\nNICHTS GEGEN DICH.", color = Paper, fontSize = 30.sp, lineHeight = 34.sp,
        fontWeight = FontWeight.Black)
    Text("Beispielbericht / alle Funde frei erfunden", color = Lime, fontSize = 13.sp)
    ScanScript.findings.forEachIndexed { index, finding ->
        Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(12.dp)).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("0${index + 1} / ${finding.label}", color = Muted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            Text(finding.value, color = if (index > 2) Lime else Paper, fontWeight = FontWeight.Black, fontSize = 25.sp)
            Text(finding.description, color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
    Text("0 B gesendet. 0 echte Funde. 100% verschwendete Lebenszeit.", color = Lime, fontSize = 13.sp)
}

@Composable
fun Radar(progress: Float, running: Boolean, modifier: Modifier = Modifier) {
    // Read the changing angle in the draw phase to avoid recomposing the whole screen every frame.
    val angle = remember { Animatable(0f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(running, lifecycle) {
        if (running) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                angle.animateTo(angle.value + 360f, tween(3000, easing = LinearEasing))
                angle.snapTo(angle.value % 360f)
            }
        }
    }
    Canvas(modifier.semantics { contentDescription = "Dekoratives Radar einer erfundenen Scan-Simulation" }) {
        val radius = size.minDimension * .42f
        val radarCenter = center
        for (ring in 1..4) drawCircle(Border, radius * ring / 4, radarCenter, style = Stroke(1.dp.toPx()))
        drawLine(Border, radarCenter - Offset(radius + 10, 0f), radarCenter + Offset(radius + 10, 0f), 1.dp.toPx())
        drawLine(Border, radarCenter - Offset(0f, radius + 10), radarCenter + Offset(0f, radius + 10), 1.dp.toPx())
        rotate(angle.value, radarCenter) {
            drawArc(Brush.sweepGradient(listOf(Color.Transparent, Lime.copy(alpha = .20f), Color.Transparent), radarCenter),
                startAngle = -80f, sweepAngle = 80f, useCenter = true,
                topLeft = radarCenter - Offset(radius, radius), size = Size(radius * 2, radius * 2))
            drawLine(Lime.copy(alpha = .7f), radarCenter, radarCenter + Offset(radius, 0f), 1.dp.toPx())
        }
        for (index in 0..5) {
            val theta = index * 1.71f + .65f
            val r = radius * (.26f + (index % 3) * .22f)
            val p = radarCenter + Offset(cos(theta) * r, sin(theta) * r)
            if (progress > index * .14f || !running) {
                drawCircle(Lime.copy(alpha = .08f), 11.dp.toPx(), p)
                drawCircle(Lime, 2.5.dp.toPx(), p)
            }
        }
        drawCircle(Ink, 13.dp.toPx(), radarCenter)
        drawCircle(Lime, 5.dp.toPx(), radarCenter, style = Stroke(2.dp.toPx()))
    }
}
