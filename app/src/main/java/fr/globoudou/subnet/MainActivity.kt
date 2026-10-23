package fr.globoudou.subnet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.globoudou.subnet.ui.AppIcons
import fr.globoudou.subnet.ui.Ipv4Screen
import fr.globoudou.subnet.ui.Ipv6Screen
import fr.globoudou.subnet.ui.SplitScreen
import fr.globoudou.subnet.ui.SubnetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { SubnetTheme { App() } }
    }
}

@Composable
private fun App() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var splitBase by rememberSaveable { mutableStateOf("192.168.1.0/24") }
    val holder = rememberSaveableStateHolder()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                val items = listOf(
                    Triple("IPv4", AppIcons.Network, 0),
                    Triple("Découpage", AppIcons.Split, 1),
                    Triple("IPv6", AppIcons.Globe, 2),
                )
                for ((label, icon, index) in items) {
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(icon, null, Modifier.size(22.dp)) },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            holder.SaveableStateProvider(tab) {
                when (tab) {
                    0 -> Ipv4Screen(onSplit = { splitBase = it; tab = 1 })
                    1 -> SplitScreen(base = splitBase, onBaseChange = { splitBase = it })
                    else -> Ipv6Screen()
                }
            }
        }
    }
}
