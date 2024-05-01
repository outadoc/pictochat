package fr.outadoc.pictochat

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject

@Composable
fun HomeScreen() {
    val viewModel = koinInject<MainViewModel>()
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        LaunchedEffect(Any()) {
            viewModel.start()
        }

        Greeting(
            name = "Android",
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        )
    }
}
