package net.drvpn.app.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import net.drvpn.app.R
import net.drvpn.app.core.ExtraHopKind
import net.drvpn.app.ui.compose.HopIcon
import net.drvpn.app.ui.compose.hopStyle

/**
 * The rescue flow when a connection doesn't work: offer emergency mode, follow its search, then
 * either celebrate or point to a premium subscription on drvpn.net.
 */
sealed interface RescueState {
    /** "Can't connect? Turn on emergency mode?" */
    data object Offer : RescueState

    /** The VPN process is trying every hop; [kind] is the one it is on. */
    data class Searching(val kind: ExtraHopKind?, val tried: Int, val total: Int) : RescueState

    /** One of them worked. */
    data class Success(val kind: ExtraHopKind?) : RescueState

    /** Nothing worked: the internet is heavily restricted; premium is the way. */
    data object Premium : RescueState
}

private val Pink = Color(0xFFDB2777)
private val Violet = Color(0xFF7C3AED)
private val Green = Color(0xFF16A34A)
private val Gold = Color(0xFFF59E0B)

@Composable
internal fun RescueDialog(
    state: RescueState,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    val searching = state is RescueState.Searching
    Dialog(
        onDismissRequest = onDismiss,
        // While searching only Back or "Hide" closes it (the search goes on in the background).
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = !searching, usePlatformDefaultWidth = false),
    ) {
        // The card pops in with a little spring each time the dialog opens.
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { shown = true }
        val pop by androidx.compose.animation.core.animateFloatAsState(
            if (shown) 1f else 0.85f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "pop",
        )
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .scale(pop)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedContent(
                targetState = state,
                contentKey = { it::class },
                transitionSpec = { (fadeIn(tween(300)) + scaleIn(initialScale = 0.92f)) togetherWith fadeOut(tween(200)) },
                label = "rescue",
            ) { s ->
                when (s) {
                    RescueState.Offer -> OfferContent(onAccept, onDismiss)
                    is RescueState.Searching -> SearchingContent(s, onDismiss)
                    is RescueState.Success -> SuccessContent(s, onDismiss)
                    RescueState.Premium -> PremiumContent(onDismiss)
                }
            }
        }
    }
}

/** A gradient header with the art in the middle. */
@Composable
private fun Header(colors: List<Color>, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(170.dp)
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Rings that grow and fade out from the middle, like a beacon. */
@Composable
private fun Beacon(color: Color, content: @Composable () -> Unit) {
    val t = rememberInfiniteTransition(label = "beacon")
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "phase")
    Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(150.dp)) {
            for (i in 0 until 3) {
                val p = (phase + i / 3f) % 1f
                drawCircle(color.copy(alpha = (1f - p) * 0.55f), radius = size.minDimension / 2 * (0.35f + 0.65f * p), style = Stroke(width = 3.dp.toPx()))
            }
        }
        content()
    }
}

@Composable
private fun OfferContent(onAccept: () -> Unit, onDismiss: () -> Unit) {
    val t = rememberInfiniteTransition(label = "shake")
    val tilt by t.animateFloat(-8f, 8f, infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "tilt")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Header(listOf(Pink, Violet)) {
            Beacon(Color.White) {
                Box(Modifier.rotate(tilt)) { HopIcon(ExtraHopKind.EMERGENCY, 72.dp) }
            }
        }
        Body(
            title = stringResource(R.string.rescue_offer_title),
            text = stringResource(R.string.rescue_offer_text),
        )
        PrimaryButton(stringResource(R.string.rescue_offer_accept), Brush.horizontalGradient(listOf(Pink, Violet)), onClick = onAccept)
        TextButton(onClick = onDismiss, modifier = Modifier.padding(bottom = 12.dp)) {
            Text(stringResource(R.string.rescue_not_now))
        }
    }
}

@Composable
private fun SearchingContent(s: RescueState.Searching, onHide: () -> Unit) {
    val t = rememberInfiniteTransition(label = "spin")
    val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "angle")
    val color = s.kind?.let { hopStyle(it).color } ?: Pink
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Header(listOf(color.copy(alpha = 0.85f), Violet)) {
            Box(Modifier.size(130.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(130.dp).rotate(angle)) {
                    drawArc(
                        brush = Brush.sweepGradient(listOf(Color.Transparent, Color.White)),
                        startAngle = 0f, sweepAngle = 300f, useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
                // The hop being tried slides in as the search moves on.
                AnimatedContent(
                    targetState = s.kind,
                    transitionSpec = { (fadeIn(tween(250)) + scaleIn(initialScale = 0.6f)) togetherWith fadeOut(tween(150)) },
                    label = "hop",
                ) { k -> HopIcon(k ?: ExtraHopKind.EMERGENCY, 76.dp) }
            }
        }
        val name = s.kind?.let { stringResource(hopStyle(it).name) } ?: stringResource(R.string.hop_emergency)
        Body(
            title = stringResource(R.string.rescue_searching_title),
            text = stringResource(R.string.rescue_searching_trying, name),
        )
        // One dot per method: done, now, still to try.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            repeat(s.total.coerceAtLeast(1)) { i ->
                val c = when {
                    i < s.tried -> MaterialTheme.colorScheme.outline
                    i == s.tried -> color
                    else -> MaterialTheme.colorScheme.outlineVariant
                }
                Box(Modifier.size(if (i == s.tried) 12.dp else 8.dp).clip(CircleShape).background(c))
            }
        }
        TextButton(onClick = onHide, modifier = Modifier.padding(bottom = 12.dp)) {
            Text(stringResource(R.string.rescue_hide))
        }
    }
}

@Composable
private fun SuccessContent(s: RescueState.Success, onDismiss: () -> Unit) {
    var go by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        go = true
        delay(6_000)
        onDismiss()
    }
    val check by androidx.compose.animation.core.animateFloatAsState(
        if (go) 1f else 0f,
        spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessLow),
        label = "check",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Header(listOf(Green, Color(0xFF0D9488))) {
            Confetti()
            Box(
                Modifier.size(96.dp).scale(check).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_action_done), contentDescription = null, tint = Green, modifier = Modifier.size(56.dp))
            }
        }
        val name = s.kind?.let { stringResource(hopStyle(it).name) }.orEmpty()
        Body(
            title = stringResource(R.string.rescue_success_title),
            text = if (name.isNotEmpty()) stringResource(R.string.rescue_success_text, name) else stringResource(R.string.rescue_success_text_plain),
        )
        PrimaryButton(stringResource(R.string.rescue_great), Brush.horizontalGradient(listOf(Green, Color(0xFF0D9488))), onClick = onDismiss)
        Spacer(Modifier.height(16.dp))
    }
}

/** Little dots that fall through the header. */
@Composable
private fun Confetti() {
    val t = rememberInfiniteTransition(label = "confetti")
    val fall by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "fall")
    val colors = listOf(Color.White, Gold, Color(0xFFA7F3D0), Color(0xFFFBCFE8))
    Canvas(Modifier.fillMaxWidth().height(170.dp)) {
        for (i in 0 until 18) {
            val x = size.width * ((i * 37 % 100) / 100f)
            val y = ((fall + i * 0.137f) % 1f) * size.height
            drawCircle(colors[i % colors.size].copy(alpha = 0.85f), radius = (3 + i % 3).dp.toPx(), center = Offset(x, y))
        }
    }
}

@Composable
private fun PremiumContent(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val t = rememberInfiniteTransition(label = "premium")
    val float by t.animateFloat(-6f, 6f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float")
    val shine by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "shine")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Header(listOf(Color(0xFF1E1B4B), Violet, Gold.copy(alpha = 0.9f))) {
            Beacon(Gold) {
                Text("👑", fontSize = 64.sp, modifier = Modifier.padding(bottom = (float + 6).dp))
            }
        }
        Body(
            title = stringResource(R.string.rescue_premium_title),
            text = stringResource(R.string.rescue_premium_text),
        )
        // The button has a light sweep running across it.
        PrimaryButton(
            stringResource(R.string.rescue_premium_button),
            Brush.linearGradient(
                listOf(Gold, Color(0xFFFDE68A), Gold),
                start = Offset(600f * shine - 200f, 0f),
                end = Offset(600f * shine + 200f, 100f),
            ),
            textColor = Color(0xFF1E1B4B),
        ) { uriHandler.openUri("https://drvpn.net") }
        TextButton(onClick = onDismiss, modifier = Modifier.padding(bottom = 12.dp)) {
            Text(stringResource(R.string.rescue_close))
        }
    }
}

@Composable
private fun Body(title: String, text: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp),
    )
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 20.dp),
    )
}

@Composable
private fun PrimaryButton(text: String, brush: Brush, textColor: Color = Color.White, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth().height(54.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(54.dp).background(brush, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            Text(text, color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
