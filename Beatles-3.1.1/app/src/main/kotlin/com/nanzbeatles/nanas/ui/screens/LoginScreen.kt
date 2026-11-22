/**
 * Vortex Music Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
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

private const val SUPABASE_URL = "https://aglwqdrunfsepsrvfjma.supabase.co"
private const val SUPABASE_PUBLISHABLE_KEY = "sb_publishable_vbNnp3EtLNgLdyjzZMW0Kg_6Ph7kTOb"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var accountName by rememberPreference(AccountNameKey, "")
    var accountEmail by rememberPreference(AccountEmailKey, "")

    var isRegisterMode by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
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
                val result = supabaseRegister(cleanName, cleanEmail, cleanPassword)
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
                val result = supabaseLogin(cleanEmail, cleanPassword)
                isLoading = false
                if (result.success) {
                    accountEmail = result.email
                    accountName = result.name
                    // Refresh Streak & Level agar status Developer langsung aktif jika ikyvortex12@gmail.com
                    StreakManager.loadAndEvaluate(context)
                    MusicJourneyManager.loadState(context)
                    Toast.makeText(context, "Berhasil masuk sebagai ${result.name}", Toast.LENGTH_SHORT).show()
                    navController.navigateUp()
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
                    "Setelah mendaftar, tunggu persetujuan (ACC) dari Developer untuk bisa masuk."
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

private suspend fun supabaseRegister(
    name: String,
    email: String,
    password: String
): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    runCatching {
        val url = URL("$SUPABASE_URL/rest/v1/vortex_users")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("apikey", SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Prefer", "return=minimal")
        }

        val isDev = DeveloperDetector.isDeveloper(email)
        val payload = JSONObject().apply {
            put("name", name)
            put("email", email)
            put("password", password)
            put("is_approved", isDev)
        }

        conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

        val code = conn.responseCode
        if (code in 200..299) {
            if (isDev) {
                true to "Akun Developer terdaftar & otomatis di-ACC! Silakan masuk."
            } else {
                true to "Pendaftaran berhasil! Tunggu ACC dari Developer agar bisa masuk."
            }
        } else {
            val err = conn.errorStream?.bufferedReader()?.readText().orEmpty()
            if (err.contains("duplicate", ignoreCase = true) || code == 409) {
                false to "Email ini sudah terdaftar. Silakan langsung masuk."
            } else {
                false to "Gagal mendaftar (Kode $code). Periksa koneksi atau konfigurasi Supabase."
            }
        }
    }.getOrElse {
        false to "Terjadi kesalahan jaringan: ${it.message}"
    }
}

private suspend fun supabaseLogin(
    email: String,
    password: String
): SupabaseLoginResult = withContext(Dispatchers.IO) {
    runCatching {
        val encodedEmail = URLEncoder.encode(email, "UTF-8")
        val encodedPassword = URLEncoder.encode(password, "UTF-8")
        val url = URL("$SUPABASE_URL/rest/v1/vortex_users?email=eq.$encodedEmail&password=eq.$encodedPassword&select=*")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("apikey", SUPABASE_PUBLISHABLE_KEY)
        }

        val code = conn.responseCode
        if (code !in 200..299) {
            return@withContext SupabaseLoginResult(false, "Gagal terhubung ke server (Kode $code).")
        }

        val responseText = conn.inputStream.bufferedReader().readText()
        val arr = JSONArray(responseText)
        if (arr.length() == 0) {
            return@withContext SupabaseLoginResult(false, "Email atau kata sandi salah.")
        }

        val user = arr.getJSONObject(0)
        val isApproved = user.optBoolean("is_approved", false)
        val userName = user.optString("name", "User")

        if (!isApproved && !DeveloperDetector.isDeveloper(email)) {
            return@withContext SupabaseLoginResult(
                false,
                "Akun Anda belum di-ACC oleh Developer. Silakan hubungi Developer via WhatsApp."
            )
        }

        SupabaseLoginResult(true, "Berhasil masuk", email = email, name = userName)
    }.getOrElse {
        SupabaseLoginResult(false, "Terjadi kesalahan jaringan: ${it.message}")
    }
}
