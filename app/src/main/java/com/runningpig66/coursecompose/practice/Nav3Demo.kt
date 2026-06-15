package com.runningpig66.coursecompose.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.runningpig66.coursecompose.ui.theme.CourseComposeTheme
import com.runningpig66.coursecompose.ui.utils.PhonePreviews

// Define keys that will identify content
data object Home
data class Product(val id: String)

@Composable
fun Nav3Demo() {
    // Create a back stack, specifying the key the app should start with
    val backStack = remember { mutableStateListOf<Any>(Home) }

    // Supply your back stack to a NavDisplay so it can reflect changes in the UI
    // ...more on this below...

    // Push a key onto the back stack (navigate forward), the navigation library will reflect the change in state
//    backStack.add(Product(id = "ABC"))

    // Pop a key off the back stack (navigate back), the navigation library will reflect the change in state
//    backStack.removeLastOrNull()

    // Create an Entry Provider function directly
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = { key ->
            when (key) {
                is Home -> NavEntry(key) {
                    ContentGreen("Welcome to Nav3") {
                        Button(onClick = {
                            backStack.add(Product("123"))
                        }) {
                            Text("Click to navigate")
                        }
                    }
                }

                is Product -> NavEntry(key, metadata = mapOf("extraDataKey" to "extraDataValue")) {
                    ContentBlue("Product ${key.id}") {}
                }

                else -> {
                    NavEntry(Unit) { Text(text = "Unknown route, Invalid Key: $it") }
                }
            }
        }
    )

    // Create an Entry Provider function use the entryProvider DSL
    /*NavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            entry<ProductList> { Text("Product List") }
            entry<ProductDetail>(metadata = mapOf("extraDataKey" to "extraDataValue")) { key ->
                Text("Product ${key.id}")
            }
        }
    )*/
}

@Composable
fun ContentGreen(
    title: String,
    content: @Composable () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFcffcc2)),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                modifier = Modifier.padding(top = 16.dp),
                fontSize = 24.sp,
                color = Color.Black
            )
            content()
        }
    }
}

@Composable
fun ContentBlue(
    title: String,
    content: @Composable () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFa8f5fd)),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                modifier = Modifier.padding(top = 16.dp),
                fontSize = 24.sp,
                color = Color.Black
            )
            content()
        }
    }
}

@PhonePreviews
@Composable
fun ContentGreenPreview() {
    CourseComposeTheme {
        ContentGreen("Welcome to Nav3") {}
    }
}

@PhonePreviews
@Composable
fun ContentBluePreview() {
    CourseComposeTheme {
        ContentBlue("Product 123") {}
    }
}
