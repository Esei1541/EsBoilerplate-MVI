package me.esei.esboilerplatemvi.base

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import me.esei.esboilerplatemvi.base.BaseContract.CommonEffect
import me.esei.esboilerplatemvi.model.DialogConfig
import kotlin.system.exitProcess

/**
 * Composable의 공통 요소를 정의한다.
 * Route 단위의 Composable에서 호출하여 최상위 요소로 사용할 것.
 * * @param viewModel 해당 Composable의 ViewModel
 * @param navController 네비게이션 처리를 위한 NavController
 * @param loadingContent 로딩 상태일 때 노출할 UI 슬롯
 * @param dialogContent 다이얼로그 상태일 때 노출할 UI 슬롯. DialogConfig를 전달받아 UI를 구성한다.
 * @param content 해당 Composable의 최상위 Layout 배치
 */
@Composable
fun <VM : BaseViewModel<*, *, *>> BaseComposable(
    viewModel: VM,
    navController: NavController,
    loadingContent: @Composable (() -> Unit)? = null,
    dialogContent: @Composable ((DialogConfig) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val commonEffect = viewModel.commonEffect.collectAsState(initial = null)
    val commonState = viewModel.commonState.collectAsState()

    //region Common Effect 동작 정의
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun showToast(@StringRes resId: Int) {
        Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show()
    }

    fun navigate(route: String, navOptions: NavOptions? = null, extras: Navigator.Extras? = null) {
        navController.navigate(route, navOptions, extras)
    }

    fun backStack() {
        navController.popBackStack()
    }

    fun popUpTo(route: String, inclusive: Boolean = false) {
        navController.popBackStack(route, inclusive)
    }

    fun finish() {
        (context as? Activity)?.finish()
    }

    fun removeTask() {
        (context as? Activity)?.finishAndRemoveTask()
    }

    fun startActivity(intent: Intent) {
        context.startActivity(intent)
    }

    fun exitProcess() {
        exitProcess(0)
    }

    fun recreate() {
        (context as? Activity)?.recreate()
    }

    fun restart() {
        (context as? Activity)?.let {
            val intent = it.packageManager.getLaunchIntentForPackage(it.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            it.startActivity(intent)
            it.finishAffinity()
            Runtime.getRuntime().exit(0)
        }
    }
    //endregion

    BackHandler {
        viewModel.onBackPressed()
    }

    // Effect 처리
    LaunchedEffect(key1 = commonEffect.value) {
        commonEffect.value?.let { flow ->
            when (flow.effect) {
                is CommonEffect.Toast -> showToast(flow.effect.message)
                is CommonEffect.ToastRes -> showToast(flow.effect.resId)
                is CommonEffect.Navigate -> navigate(flow.effect.route, flow.effect.navOptions, flow.effect.extras)
                is CommonEffect.BackStack -> backStack()
                is CommonEffect.PopUpTo -> popUpTo(flow.effect.route, flow.effect.inclusive)
                is CommonEffect.Finish -> finish()
                is CommonEffect.RemoveTask -> removeTask()
                is CommonEffect.StartActivity -> startActivity(flow.effect.intent)
                is CommonEffect.ExitProcess -> exitProcess()
                is CommonEffect.Recreate -> recreate()
                is CommonEffect.Restart -> restart()
            }
        }
    }

    // 화면 진입/이탈 감지
    DisposableEffect(key1 = navController) {
        val listener = NavController.OnDestinationChangedListener { _, _, _ ->
            viewModel.onComposeLaunched()
        }
        navController.addOnDestinationChangedListener(listener)

        onDispose {
            viewModel.onComposeDispose()
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    // 생명주기 감지
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> viewModel.onLocalLifecycleCreate()
                Lifecycle.Event.ON_START -> viewModel.onLocalLifecycleStart()
                Lifecycle.Event.ON_RESUME -> viewModel.onLocalLifecycleResume()
                Lifecycle.Event.ON_PAUSE -> viewModel.onLocalLifecyclePause()
                Lifecycle.Event.ON_STOP -> viewModel.onLocalLifecycleStop()
                Lifecycle.Event.ON_DESTROY -> viewModel.onLocalLifecycleDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    SideEffect {
        viewModel.onSideEffect()
    }

    // 레이아웃 구성
    Box(modifier = Modifier.fillMaxSize()) {
        content()

        // 다이얼로그 처리 (외부에서 주입받은 UI 노출)
        commonState.value.dialogConfig?.let { config ->
            dialogContent?.invoke(config)
        }

        // 로딩 처리 (외부에서 주입받은 UI 노출)
        if (commonState.value.isLoading) {
            loadingContent?.invoke()
        }
    }

}