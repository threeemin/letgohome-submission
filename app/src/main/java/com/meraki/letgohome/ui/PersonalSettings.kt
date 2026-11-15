package com.meraki.letgohome.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal const val DefaultVehicleName = "아이오닉 5"
internal const val DefaultDisplayId = "차박러"

private enum class ProfileField(val label: String, val action: String) {
    VEHICLE("차량 모델명", "내 자동차 변경하기"),
    DISPLAY_ID("표시 아이디", "아이디 변경하기"),
}

@Composable
internal fun PersonalSettings(
    vehicleName: String,
    displayId: String,
    onVehicleChange: (String) -> Unit,
    onDisplayIdChange: (String) -> Unit,
) {
    var editing by rememberSaveable { mutableStateOf<ProfileField?>(null) }
    var draft by rememberSaveable { mutableStateOf("") }
    Text("개인 설정", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    EnergyPanel {
        Text("내 자동차", style = MaterialTheme.typography.labelLarge)
        Text(vehicleName, style = MaterialTheme.typography.titleLarge)
        TextButton(
            onClick = { draft = vehicleName; editing = ProfileField.VEHICLE },
            modifier = Modifier.heightIn(min = 48.dp),
        ) { Text(ProfileField.VEHICLE.action) }
    }
    EnergyPanel {
        Text("내 아이디", style = MaterialTheme.typography.labelLarge)
        Text(displayId, style = MaterialTheme.typography.titleLarge)
        TextButton(
            onClick = { draft = displayId; editing = ProfileField.DISPLAY_ID },
            modifier = Modifier.heightIn(min = 48.dp),
        ) { Text(ProfileField.DISPLAY_ID.action) }
    }

    editing?.let { field ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(field.action) },
            text = {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = draft,
                    onValueChange = { if (it.length <= 30) draft = it },
                    label = { Text(field.label) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (field == ProfileField.VEHICLE) onVehicleChange(draft.trim())
                        else onDisplayIdChange(draft.trim())
                        editing = null
                    },
                    enabled = draft.isNotBlank(),
                ) { Text("저장") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("취소") } },
        )
    }
}
