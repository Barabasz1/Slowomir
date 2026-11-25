package com.zpi.slowomir.ui.screens.permissions

import com.zpi.slowomir.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zpi.slowomir.ui.components.BackIconButton
import com.zpi.slowomir.ui.components.ScreenScaffold
import com.zpi.slowomir.utils.PermissionUtils

@Composable
fun ManagePermissionsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasAccessibility by remember { mutableStateOf(PermissionUtils.isAccessibilityServiceEnabled(context)) }
    var hasOverlay by remember { mutableStateOf(PermissionUtils.isOverlayPermissionGranted(context)) }
    var hasAudio by remember { mutableStateOf(PermissionUtils.isPermissionGranted(context, Manifest.permission.RECORD_AUDIO)) }
    var hasContacts by remember { mutableStateOf(PermissionUtils.isPermissionGranted(context, Manifest.permission.READ_CONTACTS)) }
    var hasCall by remember { mutableStateOf(PermissionUtils.isPermissionGranted(context, Manifest.permission.CALL_PHONE)) }
    var hasAnswerCalls by remember { mutableStateOf(PermissionUtils.isPermissionGranted(context, Manifest.permission.ANSWER_PHONE_CALLS)) }
    var hasNotifications by remember { mutableStateOf(PermissionUtils.isPermissionGranted(context, Manifest.permission.POST_NOTIFICATIONS)) }

    fun refreshPermissions() {
        hasAccessibility = PermissionUtils.isAccessibilityServiceEnabled(context)
        hasOverlay = PermissionUtils.isOverlayPermissionGranted(context)
        hasAudio = PermissionUtils.isPermissionGranted(context, Manifest.permission.RECORD_AUDIO)
        hasContacts = PermissionUtils.isPermissionGranted(context, Manifest.permission.READ_CONTACTS)
        hasCall = PermissionUtils.isPermissionGranted(context, Manifest.permission.CALL_PHONE)
        hasAnswerCalls = PermissionUtils.isPermissionGranted(context, Manifest.permission.ANSWER_PHONE_CALLS)
        hasNotifications = PermissionUtils.isPermissionGranted(context, Manifest.permission.POST_NOTIFICATIONS)
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

    val notificationsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { refreshPermissions() }

    val permissionsList = listOf(
        PermissionItem(
            type = PermissionType.ACCESSIBILITY,
            isGranted = hasAccessibility,
            icon = Icons.Filled.Build
        ),
        PermissionItem(
            type = PermissionType.OVERLAY,
            isGranted = hasOverlay,
            icon = Icons.Filled.Settings
        ),
        PermissionItem(
            type = PermissionType.RECORD_AUDIO,
            isGranted = hasAudio,
            icon = Icons.Filled.PlayArrow,
            manifestPermission = Manifest.permission.RECORD_AUDIO
        ),
        PermissionItem(
            type = PermissionType.READ_CONTACTS,
            isGranted = hasContacts,
            icon = Icons.Filled.Person,
            manifestPermission = Manifest.permission.READ_CONTACTS
        ),
        PermissionItem(
            type = PermissionType.CALL_PHONE,
            isGranted = hasCall,
            icon = Icons.Filled.Phone,
            manifestPermission = Manifest.permission.CALL_PHONE
        ),
        PermissionItem(
            type = PermissionType.ANSWER_PHONE_CALLS,
            isGranted = hasAnswerCalls,
            icon = Icons.Filled.Call,
            manifestPermission = Manifest.permission.ANSWER_PHONE_CALLS
        ),
        PermissionItem(
            type = PermissionType.NOTIFICATIONS,
            isGranted = hasNotifications,
            icon = Icons.Filled.Notifications,
            manifestPermission = Manifest.permission.POST_NOTIFICATIONS
        )
    )

    val scrollState = rememberScrollState()
    ScreenScaffold(
        title = stringResource(R.string.permissions_manage_title),
        scrollState = scrollState,
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        navigationIcon = {
            BackIconButton(
                onClick = onBack,
                contentDescription = stringResource(R.string.permissions_back)
            )
        }
    ) { innerPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(2.dp))
        permissionsList.sortedBy { it.isGranted }.forEach { item ->
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
                        PermissionType.NOTIFICATIONS -> notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
    }
}
