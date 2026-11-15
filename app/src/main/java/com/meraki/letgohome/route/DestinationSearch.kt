package com.meraki.letgohome.route

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.meraki.letgohome.ui.CampBlue
import com.meraki.letgohome.ui.CampWarning
import kotlinx.coroutines.CancellationException

@Composable
internal fun DestinationSearch(
    demo: Boolean,
    selectedName: String?,
    onSelect: (DestinationPlace) -> Unit,
) {
    val focus = LocalFocusManager.current
    var query by rememberSaveable { mutableStateOf(selectedName.orEmpty()) }
    var request by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<DestinationPlace>?>(null) }
    var error by remember { mutableStateOf(false) }

    fun submit() {
        val term = query.trim()
        if (term.isEmpty() || request != null) return
        focus.clearFocus()
        results = null
        error = false
        request = term
    }

    LaunchedEffect(request) {
        val term = request ?: return@LaunchedEffect
        try {
            results = searchDestinations(term, demoOnly = demo)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = true
        }
        request = null
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = query,
            onValueChange = {
                if (it.length <= 100) {
                    query = it
                    request = null
                    results = null
                    error = false
                }
            },
            label = { Text("장소 또는 주소 검색") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submit() }),
            trailingIcon = {
                TextButton(onClick = ::submit, enabled = query.isNotBlank() && request == null) {
                    Text("검색")
                }
            },
        )
        if (request != null) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (error) Text("장소를 검색하지 못했습니다. 연결을 확인하고 다시 시도해 주세요.", color = CampWarning)
        results?.let { places ->
            if (places.isEmpty()) {
                Text("검색 결과가 없습니다. 다른 장소명이나 주소를 입력해 주세요.", color = CampBlue)
            } else {
                Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 2.dp) {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 146.dp)) {
                        items(places) { place ->
                            Column(
                                Modifier.fillMaxWidth().clickable {
                                    query = place.name
                                    results = null
                                    onSelect(place)
                                }.padding(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                Text(place.name, style = MaterialTheme.typography.titleSmall)
                                if (place.address.isNotBlank() && place.address != place.name) {
                                    Text(place.address, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
