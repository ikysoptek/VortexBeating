/**
 * Vortex Music Project (C) 2026
 */

package com.ikyvortex.vortexmusic.ui.screens

import android.annotation.SuppressLint
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.navigation.NavController
import com.ikyvortex.vortexmusic.LocalPlayerAwareWindowInsets
import com.ikyvortex.vortexmusic.R
import com.ikyvortex.vortexmusic.constants.AccountEmailKey
import com.ikyvortex.vortexmusic.constants.AccountNameKey
import com.ikyvortex.vortexmusic.constants.DeveloperDetector
import com.ikyvortex.vortexmusic.streak.MusicJourneyManager
import com.ikyvortex.vortexmusic.streak.StreakManager
import com.ikyvortex.vortexmusic.ui.component.IconButton
import com.ikyvortex.vortexmusic.ui.utils.backToMain
import com.ikyvortex.vortexmusic.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

val VortexLoggedInKey = booleanPreferencesKey("vortexSupabaseLoggedIn")

private const val SUPABASE_URL = "https://aglwqdrunfsepsrvfjma.supabase.co"
private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFnbHdxZHJ1bmZzZXBzcnZmam1hIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTUzMTAzNjQsImV4cCI6MjExMDg4NjM2NH0.auoxFHN4mm7gS44Lgk1sr1LeaHSefb_z7s987DaJ2Yg"
private const val DEV_WHATSAPP_NUMBER = "6285835356933"

// Palet Warna Notebook Neo-Brutalist
private val PaperBg = Color(0xFFF3F6FB)
private val PaperLine = Color(0xFFDCE4F0)
private val PaperMarginLine = Color(0xFFE7A9BE)
private val CardYellow = Color(0xFFFFF2AD)
private val NeoNavy = Color(0xFF1D2948)
private val NeoTealStart = Color(0xFF1A7C8C)
private val NeoTealEnd = Color(0xFF2896A6)
private val NeoBadgeTeal = Color(0xFF1E8494)
private val NeoHighlighter = Color(0xFFA7E3D8)
private val NeoMintBtn = Color(0xFFD8F5E5)
private val NeoMutedText = Color(0xFF5A6782)

private fun Modifier.neoBox(
    fillColor: Color,
    cornerRadius: Dp = 12.dp,
    borderWidth: Dp = 2.dp,
    shadowOffset: Dp = 3.dp,
): Modifier = this.drawBehind {
    val radius = cornerRadius.toPx()
    val offset = shadowOffset.toPx()
    val stroke = borderWidth.toPx()
    drawRoundRect(
        color = NeoNavy,
        topLeft = Offset(offset, offset),
        size = size,
        cornerRadius = CornerRadius(radius, radius)
    )
    drawRoundRect(
        color = fillColor,
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = CornerRadius(radius, radius)
    )
    drawRoundRect(
        color = NeoNavy,
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = CornerRadius(radius, radius),
        style = Stroke(width = stroke)
    )
}

private fun Modifier.neoGradientBox(
    brush: Brush,
    cornerRadius: Dp = 12.dp,
    borderWidth: Dp = 2.dp,
    shadowOffset: Dp = 4.dp,
): Modifier = this.drawBehind {
    val radius = cornerRadius.toPx()
    val offset = shadowOffset.toPx()
    val stroke = borderWidth.toPx()
    drawRoundRect(
        color = NeoNavy,
        topLeft = Offset(offset, offset),
        size = size,
        cornerRadius = CornerRadius(radius, radius)
    )
    drawRoundRect(
        brush = brush,
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = CornerRadius(radius, radius)
    )
    drawRoundRect(
        color = NeoNavy,
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = CornerRadius(radius, radius),
        style = Stroke(width = stroke)
    )
}

@SuppressLint("HardwareIds")
@Composable
fun LoginScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()

    val currentDeviceId = remember {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
    }

    var isVortexLoggedIn by rememberPreference(VortexLoggedInKey, false)
    var accountName by rememberPreference(AccountNameKey, "")
    var accountEmail by rememberPreference(AccountEmailKey, "")

    var isRegisterMode by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    fun handleSubmit() {
        val cleanEmail = emailInput.trim().lowercase()
        val cleanPassword = passwordInput.trim()
        val cleanName = nameInput.trim()

        if (cleanEmail.isBlank() || cleanPassword.isBlank() || (isRegisterMode && cleanName.isBlank())) {
            statusMessage = "Harap isi semua kolom terlebih dahulu."
            isError = true
            return
        }

        coroutineScope.launch {
            isLoading = true
            statusMessage = null
            isError = false

            if (isRegisterMode) {
                val result = supabaseRegister(cleanName, cleanEmail, cleanPassword, currentDeviceId)
                isLoading = false
                if (result.first) {
                    statusMessage = result.second
                    isError = false
                    isRegisterMode = false
                } else {
                    statusMessage = result.second
                    isError = true
                }
            } else {
                val result = supabaseLogin(cleanEmail, cleanPassword, currentDeviceId)
                isLoading = false
                if (result.success) {
                    accountEmail = result.email
                    accountName = result.name
                    isVortexLoggedIn = true
                    StreakManager.loadAndEvaluate(context)
                    MusicJourneyManager.loadState(context)
                    Toast.makeText(context, "Berhasil masuk sebagai ${result.name}", Toast.LENGTH_SHORT).show()
                    if (navController.previousBackStackEntry != null) {
                        navController.navigateUp()
                    }
                } else {
                    statusMessage = result.message
                    isError = true
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperBg)
    ) {
        // Latar Kertas Buku Bergaris + Garis Margin Kiri
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineSpacing = 28.dp.toPx()
            var y = lineSpacing
            while (y < size.height) {
                drawLine(
                    color = PaperLine,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
                y += lineSpacing
            }
            val marginX = 24.dp.toPx()
            drawLine(
                color = PaperMarginLine,
                start = Offset(marginX, 0f),
                end = Offset(marginX, size.height),
                strokeWidth = 1.5.dp.toPx()
            )
            // Aksen kelopak pastel kecil
            drawCircle(
                color = Color(0xFFF6C2D0),
                radius = 7.dp.toPx(),
                center = Offset(size.width * 0.88f, size.height * 0.09f)
            )
            drawCircle(
                color = Color(0xFFFCE28B),
                radius = 8.dp.toPx(),
                center = Offset(marginX, size.height * 0.84f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (navController.previousBackStackEntry != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                            tint = NeoNavy
                        )
                    }
                }
            }

            // Kartu Kuning + Logo Bulat di Atasnya
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                // Logo Bulat Overlapping di Atas Kartu
                Box(
                    modifier = Modifier
                        .zIndex(2f)
                        .size(86.dp)
                        .drawBehind {
                            val offset = 3.dp.toPx()
                            drawCircle(
                                color = NeoNavy,
                                radius = size.minDimension / 2f,
                                center = Offset(center.x + offset, center.y + offset)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = size.minDimension / 2f,
                                center = center
                            )
                            drawCircle(
                                color = NeoNavy,
                                radius = size.minDimension / 2f,
                                center = center,
                                style = Stroke(width = 2.5.dp.toPx())
                            )
                        }
                        .padding(5.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.vortex_icon),
                        contentDescription = "Vortex Music",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                // Kartu Kuning Utama
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 43.dp)
                        .drawBehind {
                            val radius = 16.dp.toPx()
                            drawRoundRect(
                                color = Color(0x221D2948),
                                topLeft = Offset(0f, 5.dp.toPx()),
                                size = size,
                                cornerRadius = CornerRadius(radius, radius)
                            )
                            drawRoundRect(
                                color = CardYellow,
                                topLeft = Offset.Zero,
                                size = size,
                                cornerRadius = CornerRadius(radius, radius)
                            )
                        }
                        .padding(top = 56.dp, start = 20.dp, end = 20.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Badge Garis Putus-Putus
                    Box(
                        modifier = Modifier
                            .drawBehind {
                                val radius = 8.dp.toPx()
                                drawRoundRect(
                                    color = Color(0xFFFAF6C8),
                                    cornerRadius = CornerRadius(radius, radius)
                                )
                                drawRoundRect(
                                    color = NeoNavy.copy(alpha = 0.65f),
                                    cornerRadius = CornerRadius(radius, radius),
                                    style = Stroke(
                                        width = 1.2.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f), 0f)
                                    )
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (isRegisterMode) "REGISTER · DAFTAR" else "LOGIN · MASUK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = NeoNavy
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Judul VORTEX MUSIC dengan Highlighter
                    Box(
                        modifier = Modifier.width(IntrinsicSize.Max),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .padding(bottom = 2.dp)
                                .background(NeoHighlighter, RoundedCornerShape(4.dp))
                        )
                        Text(
                            text = "VORTEX MUSIC",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = NeoNavy
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "STREAMING MUSIK · VORTEX",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp,
                        color = NeoMutedText
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    // Field Nama (Mode Daftar)
                    AnimatedVisibility(visible = isRegisterMode) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            NumberedFieldLabel(number = "1", label = "NAMA PENGGUNA")
                            Spacer(modifier = Modifier.height(8.dp))
                            NeoTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                placeholder = "Nama kamu",
                                keyboardType = KeyboardType.Text
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Field Email
                    NumberedFieldLabel(
                        number = if (isRegisterMode) "2" else "1",
                        label = "EMAIL"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    NeoTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        placeholder = "nama@email.com",
                        keyboardType = KeyboardType.Email
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Field Password
                    NumberedFieldLabel(
                        number = if (isRegisterMode) "3" else "2",
                        label = "PASSWORD"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    NeoTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        placeholder = "••••••••",
                        keyboardType = KeyboardType.Password,
                        isPassword = true,
                        showPassword = showPassword,
                        onTogglePassword = { showPassword = !showPassword }
                    )

                    // Kotak Pesan Status / Error
                    statusMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isError) Color(0xFFFFE4E6) else Color(0xFFDCFCE7)
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = msg,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isError) Color(0xFFBE123C) else Color(0xFF166534),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Tombol Utama MASUK → / DAFTAR →
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .neoGradientBox(
                                brush = Brush.horizontalGradient(listOf(NeoTealStart, NeoTealEnd))
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isLoading) { handleSubmit() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = if (isRegisterMode) "DAFTAR →" else "MASUK →",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tombol Sekunder Putih (Ganti Mode Masuk / Daftar)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .neoBox(fillColor = Color.White)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                isRegisterMode = !isRegisterMode
                                statusMessage = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRegisterMode) "Sudah punya akun? Masuk" else "Belum punya akun? Daftar",
                            color = NeoNavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Footer Kontak Developer
            Text(
                text = "Butuh bantuan? Hubungi developer",
                fontSize = 13.sp,
                color = NeoMutedText,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .neoBox(fillColor = NeoMintBtn, cornerRadius = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { uriHandler.openUri("https://wa.me/$DEV_WHATSAPP_NUMBER") }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.whatsapp),
                        contentDescription = null,
                        tint = NeoNavy,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+$DEV_WHATSAPP_NUMBER",
                        color = NeoNavy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun NumberedFieldLabel(
    number: String,
    label: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(NeoBadgeTeal),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = NeoNavy,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun NeoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .neoBox(fillColor = Color.White, cornerRadius = 12.dp, borderWidth = 2.dp, shadowOffset = 3.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = NeoNavy,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(NeoTealStart),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                visualTransformation = if (isPassword && !showPassword) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (isPassword && onTogglePassword != null) {
            Text(
                text = if (showPassword) "Tutup" else "Lihat",
                color = NeoTealStart,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onTogglePassword() }
                    .padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
            )
        }
    }
}

private data class SupabaseLoginResult(
    val success: Boolean,
    val message: String,
    val email: String = "",
    val name: String = ""
)

private fun HttpURLConnection.applySupabaseHeaders() {
    setRequestProperty("apikey", SUPABASE_ANON_KEY)
    setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
}

private suspend fun supabaseRegister(
    name: String,
    email: String,
    password: String,
    deviceId: String
): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    runCatching {
        val url = URL("$SUPABASE_URL/rest/v1/vortex_users")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            applySupabaseHeaders()
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Prefer", "return=minimal")
        }

        val isDev = DeveloperDetector.isDeveloper(email)
        val payload = JSONObject().apply {
            put("name", name)
            put("email", email)
            put("password", password)
            put("is_approved", isDev)
            put("device_id", deviceId)
        }

        conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

        val code = conn.responseCode
        if (code in 200..299) {
            if (isDev) {
                true to "Akun Developer berhasil didaftarkan. Silakan masuk."
            } else {
                true to "Pendaftaran berhasil! Tunggu persetujuan Developer untuk masuk."
            }
        } else {
            val err = conn.errorStream?.bufferedReader()?.readText().orEmpty()
            if (err.contains("duplicate", ignoreCase = true) || code == 409) {
                false to "Email ini sudah terdaftar. Silakan langsung masuk."
            } else {
                false to "Gagal mendaftar (Kode $code): $err"
            }
        }
    }.getOrElse {
        false to "Terjadi kesalahan jaringan: ${it.message}"
    }
}

private suspend fun supabaseLogin(
    email: String,
    password: String,
    currentDeviceId: String
): SupabaseLoginResult = withContext(Dispatchers.IO) {
    runCatching {
        val encodedEmail = URLEncoder.encode(email, "UTF-8")
        val encodedPassword = URLEncoder.encode(password, "UTF-8")
        val url = URL("$SUPABASE_URL/rest/v1/vortex_users?email=eq.$encodedEmail&password=eq.$encodedPassword&select=*")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            applySupabaseHeaders()
        }

        val code = conn.responseCode
        if (code !in 200..299) {
            val err = conn.errorStream?.bufferedReader()?.readText().orEmpty()
            return@withContext SupabaseLoginResult(false, "Gagal terhubung (Kode $code): $err")
        }

        val responseText = conn.inputStream.bufferedReader().readText()
        val arr = JSONArray(responseText)
        if (arr.length() == 0) {
            return@withContext SupabaseLoginResult(false, "Email atau kata sandi salah.")
        }

        val user = arr.getJSONObject(0)
        val isApproved = user.optBoolean("is_approved", false)
        val userName = user.optString("name", "User")
        val savedDeviceId = user.optString("device_id", "").takeIf { it != "null" }.orEmpty()
        val isDev = DeveloperDetector.isDeveloper(email)

        if (!isApproved && !isDev) {
            return@withContext SupabaseLoginResult(
                false,
                "Akun Anda belum disetujui. Silakan hubungi Developer."
            )
        }

        if (!isDev) {
            if (savedDeviceId.isBlank()) {
                bindDeviceIdToUser(encodedEmail, currentDeviceId)
            } else if (savedDeviceId != currentDeviceId) {
                return@withContext SupabaseLoginResult(
                    false,
                    "Akses ditolak! Akun ini sudah terikat di perangkat lain."
                )
            }
        }

        SupabaseLoginResult(true, "Berhasil masuk", email = email, name = userName)
    }.getOrElse {
        SupabaseLoginResult(false, "Terjadi kesalahan jaringan: ${it.message}")
    }
}

private fun bindDeviceIdToUser(encodedEmail: String, deviceId: String) {
    runCatching {
        val url = URL("$SUPABASE_URL/rest/v1/vortex_users?email=eq.$encodedEmail")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "PATCH"
            doOutput = true
            applySupabaseHeaders()
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Prefer", "return=minimal")
        }
        val payload = JSONObject().apply {
            put("device_id", deviceId)
        }
        conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        conn.responseCode
    }
}
