package org.kenjinx.android.views

import android.view.InputDevice
import android.view.KeyEvent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.kenjinx.android.GamePadButtonInputId
import org.kenjinx.android.controllers.ControllerButtonMappings
import org.kenjinx.android.controllers.ControllerMappingCapture
import org.kenjinx.android.controllers.NamedControllerProfiles
import org.kenjinx.android.viewmodels.QuickSettings

private fun connectedGamepads(): List<InputDevice> =
    InputDevice.getDeviceIds().mapNotNull { InputDevice.getDevice(it) }
        .filter { it.isGamepad }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControllerSettingsView(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { QuickSettings(context as android.app.Activity) }

    var switchLayout by remember { mutableStateOf(settings.useSwitchLayout) }
    var sensitivity by remember { mutableFloatStateOf(settings.controllerStickSensitivity) }
    var deadzone by remember { mutableFloatStateOf(settings.controllerStickDeadzone) }
    var profileName by remember { mutableStateOf("") }
    var selectedProfile by remember { mutableStateOf(NamedControllerProfiles.activeName(context)) }
    var profiles by remember { mutableStateOf(NamedControllerProfiles.names(context)) }
    var devices by remember { mutableStateOf(connectedGamepads()) }
    var descriptor by remember { mutableStateOf(devices.firstOrNull()?.descriptor ?: "") }
    var mappings by remember {
        mutableStateOf(ControllerButtonMappings.getMappings(context, descriptor))
    }
    var capturing by remember { mutableStateOf<GamePadButtonInputId?>(null) }
    var message by remember { mutableStateOf("") }

    DisposableEffect(context) {
        ControllerMappingCapture.listener = { device, key, target ->
            descriptor = device
            mappings = ControllerButtonMappings.getMappings(context, device)
            capturing = null
            message = "${target.name} associado ao botão ${KeyEvent.keyCodeToString(key)}"
        }
        onDispose {
            ControllerMappingCapture.cancel()
            ControllerMappingCapture.listener = null
        }
    }

    fun selectDevice(device: InputDevice) {
        descriptor = device.descriptor
        mappings = ControllerButtonMappings.getMappings(context, descriptor)
        message = "Controle selecionado: ${device.name}"
    }

    fun applyProfile(name: String) {
        val targetDescriptor = NamedControllerProfiles.descriptorFor(context, name)
        if (targetDescriptor.isBlank()) {
            message = "Este perfil ainda não possui um controle associado."
            return
        }

        ControllerButtonMappings.clearProfile(context, targetDescriptor)
        NamedControllerProfiles.loadMappings(context, name).forEach { (key, target) ->
            ControllerButtonMappings.setMapping(context, targetDescriptor, key, target)
        }

        descriptor = targetDescriptor
        mappings = ControllerButtonMappings.getMappings(context, descriptor)
        selectedProfile = name
        NamedControllerProfiles.setActive(context, name)
        message = "Perfil $name aplicado."
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Controles") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Voltar") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Layout", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = switchLayout,
                        onClick = { switchLayout = true },
                        label = { Text("Nintendo Switch") }
                    )
                    FilterChip(
                        selected = !switchLayout,
                        onClick = { switchLayout = false },
                        label = { Text("Xbox") }
                    )
                }
            }

            item {
                Text("Sensibilidade: ${"%.2f".format(sensitivity)}")
                Slider(
                    value = sensitivity,
                    onValueChange = { sensitivity = it },
                    valueRange = 0.1f..3f
                )
                Text("Deadzone: ${"%.2f".format(deadzone)}")
                Slider(
                    value = deadzone,
                    onValueChange = { deadzone = it },
                    valueRange = 0f..0.45f
                )
                Button(onClick = {
                    settings.useSwitchLayout = switchLayout
                    settings.controllerStickSensitivity = sensitivity
                    settings.controllerStickDeadzone = deadzone
                    settings.save()
                    message = "Configurações salvas."
                }) { Text("Salvar configurações") }
            }

            item {
                HorizontalDivider()
                Text("Controles conectados", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { devices = connectedGamepads() }) {
                    Text("Atualizar controles")
                }
            }

            items(devices) { device ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(device.name, modifier = Modifier.weight(1f))
                    TextButton(onClick = { selectDevice(device) }) {
                        Text(if (descriptor == device.descriptor) "Selecionado" else "Usar")
                    }
                }
            }

            item {
                Text("Mapeamento de botões", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (descriptor.isBlank()) "Conecte um controle físico para mapear."
                    else "Controle: $descriptor",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            items(GamePadButtonInputId.entries.filter {
            it != GamePadButtonInputId.None &&
            it != GamePadButtonInputId.LeftStick &&
            it != GamePadButtonInputId.RightStick
        }) { target ->
                val physicalKey = mappings.entries.firstOrNull { it.value == target }?.key
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(target.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                physicalKey?.let { KeyEvent.keyCodeToString(it) }
                                    ?: "Não mapeado",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Button(
                            enabled = descriptor.isNotBlank(),
                            onClick = {
                                capturing = target
                                ControllerMappingCapture.begin(target)
                                message = "Pressione o botão físico para ${target.name}."
                            }
                        ) {
                            Text(if (capturing == target) "Aguardando..." else "Capturar")
                        }
                    }
                }
            }

            item {
                if (capturing != null) {
                    TextButton(onClick = {
                        ControllerMappingCapture.cancel()
                        capturing = null
                        message = "Captura cancelada."
                    }) { Text("Cancelar captura") }
                }

                HorizontalDivider()
                Text("Perfis de controle", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Nome do perfil") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    enabled = profileName.isNotBlank() && descriptor.isNotBlank(),
                    onClick = {
                        NamedControllerProfiles.save(
                            context,
                            profileName,
                            descriptor,
                            mappings
                        )
                        selectedProfile = profileName.trim()
                        NamedControllerProfiles.setActive(context, selectedProfile)
                        profiles = NamedControllerProfiles.names(context)
                        message = "Perfil salvo com ${mappings.size} mapeamentos."
                        profileName = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Salvar perfil atual") }
            }

            items(profiles) { name ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (selectedProfile == name) "Perfil ativo" else "Perfil salvo",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row {
                            TextButton(onClick = { applyProfile(name) }) {
                                Text("Aplicar")
                            }
                            TextButton(onClick = {
                                NamedControllerProfiles.delete(context, name)
                                profiles = NamedControllerProfiles.names(context)
                                if (selectedProfile == name) {
                                    selectedProfile = "Padrão"
                                }
                                message = "Perfil excluído."
                            }) { Text("Excluir") }
                        }
                    }
                }
            }

            item {
                if (message.isNotBlank()) {
                    Text(message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
