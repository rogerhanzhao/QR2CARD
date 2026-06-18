package com.calb.qr2card

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calb.qr2card.ui.CardViewModel
import com.calb.qr2card.ui.QR2CardApp
import com.calb.qr2card.ui.QR2CardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QR2CardTheme {
                val viewModel: CardViewModel = viewModel()
                val context = LocalContext.current
                LaunchedEffect(Unit) {
                    viewModel.loadTemplate(context)
                }
                QR2CardApp(viewModel = viewModel)
            }
        }
    }
}
