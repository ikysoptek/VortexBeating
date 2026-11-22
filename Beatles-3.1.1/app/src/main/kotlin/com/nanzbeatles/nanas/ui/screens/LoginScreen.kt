/**
 * Vortex Music Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.screens

import android.annotation.SuppressLint
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.navigation.NavController
import com.nanzbeatles.nanas.LocalPlayerAwareWindowInsets
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.constants.AccountEmailKey
import com.nanzbeatles.nanas.constants.AccountNameKey
import com.nanzbeatles.nanas.constants.DeveloperDetector
import com.nanzbeatles.nanas.streak.MusicJourneyManager
import com.nanzbeatles.nanas.streak.StreakManager
import com.nanzbeatles.nanas.ui.component.IconButton
import com.nanzbeatles.nanas.ui.utils.backToMain
import com.nanzbeatles.nanas.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// Kunci khusus status login Supabase (tidak bentrok dengan data YouTube lama)
val VortexLoggedInKey = booleanPreferencesKey("vortexSupabaseLoggedIn")

private const val SUPABASE_URL = "https://aglwqdrunfsepsrvfjma.supabase.co"
private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFnbHdxZHJ1bmZzZXBzcnZmam1hIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTUzMTAzNjQsImV4cCI6MjExMDg4NjM2NH0.auoxFHN4mm7gS44Lgk1sr1LeaHSefb_z7s987DaJ2Yg"
private const val DEV_WHATSAPP_NUMBER = "6285835356933" // Ganti dengan nomor WA kamu

@SuppressLint("HardwareIds")
@OptIn(ExperimentalMaterial3Api::class)
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
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    fun openWhatsAppDeveloper() {
        val emailText = emailInput.trim().ifBlank { "(belum diisi)" }
        val message = "Halo Developer Vortex Music, saya sudah mendaftar dengan email: $emailText. Mohon ACC akun saya."
        val encodedMsg = URLEncoder.encode(message, "UTF-8")
        uriHandler.openUri("https://wa.me/$DEV_WHATSAPP_NUMBER?text=$encodedMsg")
    }

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

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current),
        topBar = {
            TopAppBar(
                title = { Text(if (isRegisterMode) "Daftar Akun Vortex" else "Masuk Vortex Music") },
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                        IconButton(
                            onClick = navController::navigateUp,
                            onLongClick = navController::backToMain
                        ) {
                            Icon(
                                painterResource(R.drawable.arrow_back),
                                contentDescription = null
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isRegisterMode) "Buat Akun Baru" else "Selamat Datang",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isRegisterMode) {
                    "Setelah mendaftar, hubungi Developer via WhatsApp untuk persetujuan (ACC). 1 akun hanya berlaku untuk 1 perangkat."
                } else {
                    "Masuk menggunakan akun Vortex Music yang sudah di-ACC."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isRegisterMode) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Nama Pengguna") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = emailInput,
                onValueChange = { emailInput = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text("Kata Sandi") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            statusMessage?.let { msg ->
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { handleSubmit() },
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = if (isRegisterMode) "Daftar Sekarang" else "Masuk",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { openWhatsAppDeveloper() },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.whatsapp),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Hubungi Developer (Minta ACC)")
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = {
                    isRegisterMode = !isRegisterMode
                    statusMessage = null
                }
            ) {
                Text(
                    text = if (isRegisterMode) "Sudah punya akun? Masuk di sini" else "Belum punya akun? Daftar dulu"
                )
            }
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
                true to "Akun Developer terdaftar & otomatis di-ACC! Silakan masuk."
            } else {
                true to "Pendaftaran berhasil! Klik tombol WhatsApp Developer di bawah untuk minta ACC."
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
                "Akun Anda belum di-ACC. Silakan klik tombol WhatsApp Developer di bawah."
            )
        }

        if (!isDev) {
            if (savedDeviceId.isBlank()) {
                bindDeviceIdToUser(encodedEmail, currentDeviceId)
            } else if (savedDeviceId != currentDeviceId) {
                return@withContext SupabaseLoginResult(
                    false,
                    "Akses ditolak! Akun ini sudah terikat di perangkat lain (1 akun hanya untuk 1 perangkat)."
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
