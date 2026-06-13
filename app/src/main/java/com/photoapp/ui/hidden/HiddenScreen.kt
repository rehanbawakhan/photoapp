package com.photoapp.ui.hidden

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.KeyboardBackspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.photoapp.ui.components.EmptyState
import com.photoapp.ui.components.EmptyStateType
import com.photoapp.ui.components.PhotoGrid
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat

enum class AuthState {
    SETUP_ENTER_PIN,
    SETUP_CONFIRM_PIN,
    ENTER_PIN,
    AUTHENTICATED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenScreen(
    onPhotoClick: (Long) -> Unit,
    onBack: () -> Unit,
    bottomPadding: Dp = 0.dp,
    viewModel: HiddenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Security locking logic
    val context = LocalContext.current
    var isPinSet by remember { mutableStateOf(viewModel.isPinSet()) }
    var currentAuthState by remember {
        mutableStateOf(if (isPinSet) AuthState.ENTER_PIN else AuthState.SETUP_ENTER_PIN)
    }

    // Safe retrieval of FragmentActivity
    val activity = remember(context) {
        var currentContext = context
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is FragmentActivity) {
                break
            }
            currentContext = currentContext.baseContext
        }
        currentContext as? FragmentActivity
    }

    val biometricAvailable = remember(context) {
        val biometricManager = BiometricManager.from(context)
        biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    val triggerBiometricAuth = {
        activity?.let { activeActivity ->
            val executor = ContextCompat.getMainExecutor(activeActivity)
            val biometricPrompt = BiometricPrompt(
                activeActivity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                            errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                            errorCode != BiometricPrompt.ERROR_CANCELED
                        ) {
                            android.widget.Toast.makeText(context, errString, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        viewModel.setAuthenticated(true)
                        currentAuthState = AuthState.AUTHENTICATED
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Hidden Album")
                .setSubtitle("Authenticate using your fingerprint sensor")
                .setNegativeButtonText("Use PIN")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()

            try {
                biometricPrompt.authenticate(promptInfo)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Auto trigger biometrics on entry if PIN is already set and not yet authenticated
    LaunchedEffect(isAuthenticated, isPinSet) {
        if (!isAuthenticated && isPinSet && biometricAvailable) {
            triggerBiometricAuth()
        }
    }

    if (!isAuthenticated && currentAuthState != AuthState.AUTHENTICATED) {
        HiddenAuthView(
            authState = currentAuthState,
            biometricAvailable = biometricAvailable,
            onBack = onBack,
            onPinEntered = { enteredPin ->
                when (currentAuthState) {
                    AuthState.SETUP_ENTER_PIN -> {
                        // Move to confirmation step
                        currentAuthState = AuthState.SETUP_CONFIRM_PIN
                        enteredPin // Return typed value to confirm
                    }
                    AuthState.ENTER_PIN -> {
                        val success = viewModel.verifyPin(enteredPin)
                        if (success) {
                            currentAuthState = AuthState.AUTHENTICATED
                            android.widget.Toast.makeText(context, "Album unlocked", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(context, "Incorrect PIN. Try again.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        ""
                    }
                    else -> ""
                }
            },
            onConfirmPin = { originalPin, confirmedPin ->
                if (originalPin == confirmedPin) {
                    viewModel.savePin(confirmedPin)
                    isPinSet = true
                    currentAuthState = AuthState.AUTHENTICATED
                    android.widget.Toast.makeText(context, "Security PIN created successfully", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(context, "PINs do not match. Try again.", android.widget.Toast.LENGTH_SHORT).show()
                    currentAuthState = AuthState.SETUP_ENTER_PIN
                }
            },
            onBiometricClick = {
                triggerBiometricAuth()
            }
        )
        return
    }

    // Intent sender launcher for permanent deletion on Android 10+
    val deleteIntentSender by viewModel.deleteIntentSender.collectAsState()
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onPhotosDeletedConfirm()
        } else {
            viewModel.clearDeleteIntentSender()
        }
    }

    LaunchedEffect(deleteIntentSender) {
        deleteIntentSender?.let { intentSender ->
            val intentSenderRequest = IntentSenderRequest.Builder(intentSender).build()
            deleteLauncher.launch(intentSenderRequest)
        }
    }

    BackHandler(enabled = true) {
        if (uiState.isSelectionMode) {
            viewModel.clearSelection()
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isSelectionMode) "${uiState.selectedIds.size} selected" else "Hidden",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isSelectionMode) {
                            viewModel.clearSelection()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = if (uiState.isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (uiState.isSelectionMode) {
                        IconButton(onClick = { viewModel.unhideSelected() }) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Unhide"
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = "Delete permanently",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        if (uiState.photos.isNotEmpty()) {
                            IconButton(onClick = { viewModel.selectAll() }) {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = "Select all"
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (uiState.photos.isEmpty() && !uiState.isLoading) {
            EmptyState(
                type = EmptyStateType.HIDDEN,
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(bottom = bottomPadding)
            ) {
                PhotoGrid(
                    photos = uiState.photos,
                    selectedIds = uiState.selectedIds,
                    isSelectionMode = uiState.isSelectionMode,
                    onPhotoClick = { photo ->
                        if (uiState.isSelectionMode) {
                            viewModel.toggleSelection(photo.id)
                        } else {
                            onPhotoClick(photo.id)
                        }
                    },
                    onPhotoLongClick = { photo ->
                        viewModel.toggleSelection(photo.id)
                    },
                    groupByDate = true,
                    onSelectionChanged = { viewModel.setSelectedIds(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Permanently") },
            text = {
                Text(
                    "Are you sure you want to permanently delete the selected ${uiState.selectedIds.size} items? This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSelected()
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenAuthView(
    authState: AuthState,
    biometricAvailable: Boolean,
    onBack: () -> Unit,
    onPinEntered: (String) -> String,
    onConfirmPin: (String, String) -> Unit,
    onBiometricClick: () -> Unit
) {
    var pinValue by remember { mutableStateOf("") }
    var setupOriginalPin by remember { mutableStateOf("") }

    val subtitleText = when (authState) {
        AuthState.SETUP_ENTER_PIN -> "Create a security PIN\nEnter a 4-digit PIN to secure hidden files."
        AuthState.SETUP_CONFIRM_PIN -> "Confirm your security PIN\nRe-enter the 4-digit PIN."
        AuthState.ENTER_PIN -> "Hidden Vault Locked\nEnter your 4-digit security PIN."
        else -> ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Upper Lock Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .padding(18.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (authState == AuthState.ENTER_PIN) "Hidden Vault" else "Setup Security PIN",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // PIN Entry Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < pinValue.length
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .padding(3.dp)
                        ) {
                            if (isFilled) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Numpad
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                val numpadButtons = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("biometric", "0", "delete")
                )

                numpadButtons.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { item ->
                            when (item) {
                                "biometric" -> {
                                    if (biometricAvailable && authState == AuthState.ENTER_PIN) {
                                        IconButton(
                                            onClick = onBiometricClick,
                                            modifier = Modifier
                                                .size(76.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    CircleShape
                                                )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Fingerprint,
                                                contentDescription = "Fingerprint",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(76.dp))
                                    }
                                }
                                "delete" -> {
                                    IconButton(
                                        onClick = {
                                            if (pinValue.isNotEmpty()) {
                                                pinValue = pinValue.dropLast(1)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(76.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.KeyboardBackspace,
                                            contentDescription = "Backspace",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }
                                else -> {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .clickable {
                                                if (pinValue.length < 4) {
                                                    pinValue += item
                                                    if (pinValue.length == 4) {
                                                        if (authState == AuthState.SETUP_ENTER_PIN) {
                                                            setupOriginalPin = pinValue
                                                            pinValue = ""
                                                            onPinEntered(setupOriginalPin)
                                                        } else if (authState == AuthState.SETUP_CONFIRM_PIN) {
                                                            onConfirmPin(setupOriginalPin, pinValue)
                                                            pinValue = ""
                                                            setupOriginalPin = ""
                                                        } else {
                                                            onPinEntered(pinValue)
                                                            pinValue = ""
                                                        }
                                                    }
                                                }
                                            }
                                    ) {
                                        Text(
                                            text = item,
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
