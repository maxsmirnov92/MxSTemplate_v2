package net.maxsmr.vk_news_client.ui.presentation.login

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vk.id.AccessToken
import com.vk.id.VKIDAuthFail
import com.vk.id.auth.VKIDAuthUiParams
import com.vk.id.onetap.common.OneTapStyle
import com.vk.id.onetap.compose.onetap.OneTap
import net.maxsmr.designsystem.compose.theme.AppColors.DarkBlue
import net.maxsmr.feature.vk_news_client.ui.R

@Composable
fun LoginScreen(onLoginClick: () -> Unit) {
//    val owner = LocalLifecycleOwner.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                modifier = Modifier.size(160.dp),
                painter = painterResource(id = R.drawable.ic_vk_logo),
                contentDescription = null
            )
            Spacer(Modifier.size(100.dp))
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkBlue,
                    contentColor = Color.White
                ),
                onClick = onLoginClick
            ) {
                Text(stringResource(R.string.vk_news_client_login_button))
            }
        }
    }
}

@Composable
fun LoginScreenOneTap(
    context: Context,
    snackbarHostState: SnackbarHostState,
    onAuthSuccess: (AccessToken) -> Unit,
    onAuthFailed: (VKIDAuthFail) -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) {
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
            contentAlignment = Alignment.BottomEnd
        ) {
            OneTap(
                modifier = Modifier.padding(6.dp),
                signInAnotherAccountButtonEnabled = true,
                style = OneTapStyle.system(context),
                onAuth = { _, token ->
                    onAuthSuccess(token)
                },
                onFail = { _, reason ->
                    onAuthFailed(reason)
                },
                authParams = VKIDAuthUiParams {
                    scopes = AUTH_SCOPES
                }
            )
        }
    }
}