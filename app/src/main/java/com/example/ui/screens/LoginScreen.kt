package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.BuildConfig
import com.example.R
import com.example.ui.InvoiceViewModel
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: InvoiceViewModel,
    onLoginSuccess: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoggingIn by remember { mutableStateOf(false) }
    
    // Redirect if already logged in
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            onLoginSuccess()
        }
    }

    // Bento Grid theme gradient background: soft lavender-white to slightly deeper lavender
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.surfaceVariant
        )
    )

    // Lightning pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("login_screen"),
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(innerPadding)
                .safeDrawingPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background ambient glow using primary brand color (lavender-purple)
            val primaryGlowColor = MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    primaryGlowColor.copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            ),
                            radius = size.width / 1.5f
                        )
                    }
                    .align(Alignment.Center)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
            ) {
                // Logo section with glowing neon boundary matching BentoPrimary
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(96.dp * pulseScale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Blesková Faktúra Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Brand head text
                Text(
                    text = "Blesková Faktúra",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Slovak subtext
                Text(
                    text = "Rýchla AI evidencia a automatické skenovanie účtovných dokladov pre slovenských živnostníkov.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Elegant White Bento Card for login actions
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Začnite účtovať bleskovo",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Google Sign-In Button
                        Button(
                            onClick = {
                                if (isLoggingIn) return@Button

                                if (!viewModel.isFirebaseMode) {
                                    Toast.makeText(
                                        context,
                                        "Cloudové prihlásenie nie je v tejto zostave dostupné.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@Button
                                }

                                val webClientId = try {
                                    context.getString(R.string.default_web_client_id)
                                } catch (e: Exception) {
                                    Log.e("Auth", "Missing default_web_client_id resource", e)
                                    Toast.makeText(
                                        context,
                                        "Chýba konfigurácia Google prihlásenia (Web Client ID).",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@Button
                                }

                                val activity = context as? Activity
                                if (activity == null) {
                                    Toast.makeText(
                                        context,
                                        "Prihlásenie vyžaduje aktívne okno aplikácie.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@Button
                                }

                                coroutineScope.launch {
                                    isLoggingIn = true
                                    try {
                                        val credentialManager = CredentialManager.create(context)
                                        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = webClientId)
                                            .build()

                                        val request = GetCredentialRequest.Builder()
                                            .addCredentialOption(signInOption)
                                            .build()

                                        val result = credentialManager.getCredential(activity, request)
                                        val credential = result.credential

                                        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                            val idToken = googleIdTokenCredential.idToken

                                            viewModel.signInWithGoogle(idToken) { success ->
                                                if (success) {
                                                    Toast.makeText(context, "Prihlásenie bolo úspešné!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Chyba prihlásenia vo Firebase Auth.", Toast.LENGTH_LONG).show()
                                                }
                                                isLoggingIn = false
                                            }
                                        } else {
                                            Toast.makeText(context, "Neočakávaný typ poverenia.", Toast.LENGTH_LONG).show()
                                            isLoggingIn = false
                                        }
                                    } catch (e: GetCredentialCancellationException) {
                                        Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
                                        isLoggingIn = false
                                    } catch (e: Exception) {
                                        Log.e("Auth", "Google Sign-In failed", e)
                                        Toast.makeText(context, "Prihlásenie cez Google zlyhalo: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                        isLoggingIn = false
                                    }
                                }
                            },
                            enabled = !isLoggingIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                                .testTag("google_login_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                // Beautiful colored G icon using Canvas
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .drawBehind {
                                            drawArc(
                                                color = Color(0xFFEA4335), // Red
                                                startAngle = 135f,
                                                sweepAngle = 90f,
                                                useCenter = true
                                            )
                                            drawArc(
                                                color = Color(0xFF4285F4), // Blue
                                                startAngle = -45f,
                                                sweepAngle = 180f,
                                                useCenter = true
                                            )
                                            drawArc(
                                                color = Color(0xFF34A853), // Green
                                                startAngle = 45f,
                                                sweepAngle = 90f,
                                                useCenter = true
                                            )
                                            drawArc(
                                                color = Color(0xFFFBBC05), // Yellow
                                                startAngle = 225f,
                                                sweepAngle = 40f,
                                                useCenter = true
                                            )
                                        }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Prihlásiť sa cez Google",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.2.sp
                                    )
                                )
                            }
                        }

                        if (BuildConfig.DEBUG) {
                            // Local / Demo Mode Entry (Debug builds only)
                            OutlinedButton(
                                onClick = {
                                    viewModel.signInDemoUser()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("demo_login_button"),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Hosť",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Vstúpiť ako hosť (Debug Demo)",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.2.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Connection and Verification Status Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = if (viewModel.isFirebaseMode) MaterialTheme.colorScheme.primary else Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = if (viewModel.isFirebaseMode) "Režim: Bezpečný Cloud" else "Chýba cloudová konfigurácia",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (viewModel.isFirebaseMode) {
                                    "Aplikácia je pripojená k bezpečnému cloudu Firebase. Vaše doklady sú chránené a viazané na váš Google účet."
                                } else {
                                    "Prihlásenie cez cloud nie je v tejto zostave nakonfigurované. Skontrolujte prosím konfiguráciu služby Firebase."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
