package com.android.identity_credential.wallet.ui.destination.about

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.android.identity.util.Logger
import com.android.identity_credential.wallet.LoggerModel
import com.android.identity_credential.wallet.R
import com.android.identity_credential.wallet.navigation.WalletDestination
import com.android.identity_credential.wallet.ui.ScreenWithAppBarAndBackButton

@Composable
fun AboutScreen(loggerModel: LoggerModel, onNavigate: (String) -> Unit) {
    val localUriHandler = LocalUriHandler.current
    ScreenWithAppBarAndBackButton(
        title = stringResource(R.string.about_screen_title),
        onBackButtonClick = { onNavigate(WalletDestination.PopBackStack.route) }
    ) {
        Spacer(modifier = Modifier.weight(0.5f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Column() {
                Text(
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    text = stringResource(R.string.about_screen_text)
                )
                ClickableText(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.bodyLarge,
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = Color.Blue,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(stringResource(R.string.about_screen_home_page_text))
                        }
                    },
                    onClick = { _ ->
                        localUriHandler.openUri(
                            "https://github.com/openwallet-foundation-labs/identity-credential")
                    }
                )
                ClickableText(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.bodyLarge,
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = Color.Blue,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(stringResource(R.string.about_screen_iaca_cert_text))
                        }
                    },
                    onClick = { _ ->
                        localUriHandler.openUri(
                            "https://github.com/openwallet-foundation-labs/identity-credential/blob/main/wallet/src/main/res/raw/iaca_certificate.pem")
                    }
                )
            }
        }
        Spacer(modifier = Modifier.weight(0.5f))
        if (Logger.isDebugEnabled) {
            DevelopmentTools(loggerModel)
        }
    }
}

@Composable
fun DevelopmentTools(loggerModel: LoggerModel) {
    val context = LocalContext.current
    Divider(modifier = Modifier.padding(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Development tools",
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.titleLarge,
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val logToFile = loggerModel.logToFile.observeAsState(false)
        Checkbox(checked = logToFile.value, onCheckedChange = {loggerModel.logToFile.value = it})
        Text("Log to file",
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodyLarge)
        Button(
            modifier = Modifier.padding(8.dp),
            onClick = {
                loggerModel.clearLog()
            }) {
            Text(text = "Clear")
        }
        Button(
            modifier = Modifier.padding(8.dp),
            onClick = {
                context.startActivity(Intent.createChooser(
                    loggerModel.createLogSharingIntent(context), "Share log"))
            }) {
            Text(text = "Share")
        }
    }
}