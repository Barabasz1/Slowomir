package com.zpi.slowomir.ui.screens.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zpi.slowomir.ui.screens.permissions.PermissionCard
import com.zpi.slowomir.ui.screens.permissions.PermissionItem
import com.zpi.slowomir.ui.screens.permissions.PermissionType
import com.zpi.slowomir.ui.screens.permissions.PrivacyInfoCard
import com.zpi.slowomir.ui.theme.SlowomirBlue
import com.zpi.slowomir.ui.theme.SlowomirTeal
import com.zpi.slowomir.utils.PermissionUtils

@Composable
fun OnboardingPermissionsScreen(
    onFinishClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current

    var hasAccessibility by remember { mutableStateOf(PermissionUtils.isAccessibilityServiceEnabled(context)) }
    var hasOverlay by remember { mutableStateOf(PermissionUtils.isOverlayPermissionGranted(context)) }
    var hasAudio by remember { mutableStateOf(PermissionUtils.isPermissionGranted(context, Manifest.permission.RECORD_AUDIO)) }
    var hasContacts by remember { mutableStateOf(PermissionUtils.isPermissionGranted(context, Manifest.permission.READ_CONTACTS)) }
    var hasCall by remember { mutableStateOf(PermissionUtils.isPermissionGranted(context, Manifest.permission.CALL_PHONE)) }
    var hasAnswerCalls by remember { mutableStateOf(PermissionUtils.isPermissionGranted(context, Manifest.permission.ANSWER_PHONE_CALLS)) }

    fun refreshPermissions() {
        hasAccessibility = PermissionUtils.isAccessibilityServiceEnabled(context)
        hasOverlay = PermissionUtils.isOverlayPermissionGranted(context)
        hasAudio = PermissionUtils.isPermissionGranted(context, Manifest.permission.RECORD_AUDIO)
        hasContacts = PermissionUtils.isPermissionGranted(context, Manifest.permission.READ_CONTACTS)
        hasCall = PermissionUtils.isPermissionGranted(context, Manifest.permission.CALL_PHONE)
        hasAnswerCalls = PermissionUtils.isPermissionGranted(context, Manifest.permission.ANSWER_PHONE_CALLS)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val audioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { refreshPermissions() }

    val contactsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { refreshPermissions() }

    val callLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { refreshPermissions() }

    val answerCallsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { refreshPermissions() }

    val permissionsList = listOf(
        PermissionItem(
            type = PermissionType.ACCESSIBILITY,
            title = "Usługa dostępności",
            isGranted = hasAccessibility,
            icon = Icons.Filled.Build
        ),
        PermissionItem(
            type = PermissionType.OVERLAY,
            title = "Wyświetlanie nad innymi aplikacjami",
            isGranted = hasOverlay,
            icon = Icons.Filled.Settings
        ),
        PermissionItem(
            type = PermissionType.RECORD_AUDIO,
            title = "Dostęp do mikrofonu",
            isGranted = hasAudio,
            icon = Icons.Filled.PlayArrow,
            manifestPermission = Manifest.permission.RECORD_AUDIO
        ),
        PermissionItem(
            type = PermissionType.READ_CONTACTS,
            title = "Odczyt kontaktów",
            isGranted = hasContacts,
            icon = Icons.Filled.Person,
            manifestPermission = Manifest.permission.READ_CONTACTS
        ),
        PermissionItem(
            type = PermissionType.CALL_PHONE,
            title = "Wykonywanie połączeń",
            isGranted = hasCall,
            icon = Icons.Filled.Phone,
            manifestPermission = Manifest.permission.CALL_PHONE
        ),
        PermissionItem(
            type = PermissionType.ANSWER_PHONE_CALLS,
            title = "Odbieranie połączeń",
            isGranted = hasAnswerCalls,
            icon = Icons.Filled.Call,
            manifestPermission = Manifest.permission.ANSWER_PHONE_CALLS
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Column {
            Text(
                text = "Uprawnienia aplikacji",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "SłowoMIR wymaga przyznania odpowiednich uprawnień systemowych do prawidłowego działania.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        PrivacyInfoCard()

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Włącz wymagane uprawnienia:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val scrollState = rememberScrollState()
            val maxHeight = constraints.maxHeight.toFloat()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                permissionsList.forEach { item ->
                    PermissionCard(
                        permission = item,
                        onRequestPermission = {
                            when (item.type) {
                                PermissionType.ACCESSIBILITY -> PermissionUtils.openAccessibilitySettings(context)
                                PermissionType.OVERLAY -> PermissionUtils.openOverlaySettings(context)
                                PermissionType.RECORD_AUDIO -> audioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                PermissionType.READ_CONTACTS -> contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                                PermissionType.CALL_PHONE -> callLauncher.launch(Manifest.permission.CALL_PHONE)
                                PermissionType.ANSWER_PHONE_CALLS -> answerCallsLauncher.launch(Manifest.permission.ANSWER_PHONE_CALLS)
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (scrollState.maxValue > 0) {
                val contentHeight = maxHeight + scrollState.maxValue
                val thumbHeight = (maxHeight * (maxHeight / contentHeight)).coerceAtLeast(30f)
                val scrollProgress = scrollState.value.toFloat() / scrollState.maxValue.toFloat()
                val maxOffset = maxHeight - thumbHeight
                val thumbOffset = scrollProgress * maxOffset

                val thumbHeightDp: Dp = with(density) { thumbHeight.toDp() }
                val thumbOffsetDp: Dp = with(density) { thumbOffset.toDp() }
                val trackHeightDp: Dp = with(density) { maxHeight.toDp() }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(4.dp)
                        .height(trackHeightDp)
                        .background(
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            RoundedCornerShape(2.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .width(1.dp)
                            .height(thumbHeightDp)
                            .offset(y = thumbOffsetDp)
                            .background(
                                androidx.compose.ui.graphics.Color.Gray,
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(SlowomirTeal, SlowomirBlue)
                    )
                )
                .clickable(
                    role = Role.Button,
                    onClick = onFinishClick
                )
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Przejdź do aplikacji",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
