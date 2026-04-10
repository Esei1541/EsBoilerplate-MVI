package me.esei.esboilerplatemvi.base

import android.content.Intent
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import me.esei.esboilerplatemvi.base.BaseContract.CommonEffect
import me.esei.esboilerplatemvi.base.BaseContract.CommonState
import me.esei.esboilerplatemvi.base.BaseContract.UiEffect
import me.esei.esboilerplatemvi.base.BaseContract.UiEvent
import me.esei.esboilerplatemvi.base.BaseContract.UiState
import me.esei.esboilerplatemvi.model.DialogConfig
import me.esei.esboilerplatemvi.model.WrappedEffect

/**
 * ViewModel의 공통 사항을 정의한다.
 * ViewModel Class 대신 이 클래스를 상속할 것.
 * @param dispatcher Coroutine을 실행할 기본 Dispatcher. 자식 class에서 scope 변수를 통해 접근할 수 있다. 주로 테스트 환경에서 TestDispatcher를 주입하여 사용하기 위함. null일 경우 scope로 지정되므로 자식 class 생성자에 dispatcher: CoroutineDispatcher? = null 등으로 받아 테스트 환경이 아닐 경우 값을 넘겨 주지 않는 방식으로 구현할 것.
 */
abstract class BaseViewModel<Event : UiEvent, State : UiState, Effect : UiEffect>(
    dispatcher: CoroutineDispatcher? = null
) : ViewModel() {

    protected open val TAG = javaClass.simpleName

    protected val scope: CoroutineScope = if (dispatcher == null) viewModelScope else CoroutineScope(dispatcher)

    private val initialState: State by lazy { createInitialState() }
    private val initialCommonState: CommonState by lazy { createCommonState() }

    /**
     * UI의 상태를 나타내는 불변 데이터
     * State는 항상 최신의 UI 상태를 유지하며, View에서 구독하여 자동으로 업데이트를 처리한다.
     */
    protected val _state: MutableStateFlow<State> = MutableStateFlow(initialState)
    val state = _state.asStateFlow()

    /**
     * 사용자가 UI View와 상호작용 시 발생하는 액션(클릭, 입력, 스크롤 등)을 처리한다.
     * View -> ViewModel으로 전달되며 이를 통해 상태(State)를 변경하거나 특정 효과(Effect)를 발생시킬 수 있다.
     */
    protected val _event: MutableSharedFlow<Event> = MutableSharedFlow()
    val event = _event.asSharedFlow()

    /**
     * 일시적인 UI 동작 등 일회성 작업을 처리한다.
     * ViewModel -> View로 전달되며, ViewModel에서 정의한 Effect의 효과는 View에서 LaunchedEffect(key1 = effect.value)를 정의해야 한다.
     */
    protected val _effect: Channel<WrappedEffect<Effect>> = Channel()
    val effect = _effect.receiveAsFlow()

    /**
     * 공통적으로 사용하는 State를 관리한다.
     */
    private val _commonState: MutableStateFlow<CommonState> = MutableStateFlow(initialCommonState)
    val commonState = _commonState.asStateFlow()

    /**
     * 공통적으로 사용하는 Effect를 정의한다.
     */
    private val _commonEffect: Channel<WrappedEffect<CommonEffect>> = Channel()
    val commonEffect = _commonEffect.receiveAsFlow()

    protected lateinit var savedStateHandle: SavedStateHandle

    private var backButtonEnabled: Boolean = false

    init {
        subscribeEvents()
    }

    /**
     * 프로세스 종료 후 복원 시 상태 유지를 위한 SavedStateHandle을 초기화한다.
     */
    open fun initSavedStateHandle(savedStateHandle: SavedStateHandle) {
        this.savedStateHandle = savedStateHandle
    }

    /**
     * Back Button 허용 여부를 설정한다
     */
    open fun initBackButtonSetting(disableBackButton: Boolean) {
        this.backButtonEnabled = !disableBackButton
    }

    //region Lifecycle Function
    /**
     * 연결된 Composable이 활성화 상태가 될 때 호출된다.
     */
    open fun onComposeLaunched() {}

    /**
     * 연결된 Composable이 비활성화될 때 호출된다.
     */
    open fun onComposeDispose() {}

    /**
     * 연결된 Composable에서 화면이 변경될 때마다 실행된다.
     */
    open fun onSideEffect() {}

    /**
     * LocalLifecycleOwner의 onCreate 이벤트가 발생했을 때 호출된다.
     */
    open fun onLocalLifecycleCreate() {}

    /**
     * LocalLifecycleOwner의 onStart 이벤트가 발생했을 때 호출된다.
     */
    open fun onLocalLifecycleStart() {}

    /**
     * LocalLifecycleOwner의 onResume 이벤트가 발생했을 때 호출된다.
     */
    open fun onLocalLifecycleResume() {}

    /**
     * LocalLifecycleOwner의 onPause 이벤트가 발생했을 때 호출된다.
     */
    open fun onLocalLifecyclePause() {}

    /**
     * LocalLifecycleOwner의 onStop 이벤트가 발생했을 때 호출된다.
     */
    open fun onLocalLifecycleStop() {}

    /**
     * LocalLifecycleOwner의 onDestroy 이벤트가 발생했을 때 호출된다.
     */
    open fun onLocalLifecycleDestroy() {}
    //endregion

    /**
     * Contract Class에 정의된 Event에 대한 동작을 정의한다.
     * when문과 is문 체크를 통해 작성할 것.
     */
    protected open fun handleEvent(event: Event) {}

    /**
     * 시스템 또는 UI에 의한 뒤로 가기 동작이 감지되었을 때 호출된다.
     * backButtonEnabled 상태에 따라 뒤로 가기 처리를 수행한다.
     */
    open fun onBackPressed() {
        if (!backButtonEnabled) return
        backStack()
    }

    /**
     * View에서 호출하여 특정 Event를 발생시킨다.
     */
    fun setEvent(event: Event) {
        val newEvent = event
        scope.launch { _event.emit(newEvent) }
    }

    /**
     * 특정 State에 새 값을 지정한다.
     */
    protected fun setState(reduce: State.() -> State) {
        val newState = state.value.reduce()
        _state.value = newState
    }

    /**
     * 특정 CommonState에 새 값을 지정한다.
     */
    protected fun setCommonState(reduce: CommonState.() -> CommonState) {
        val newState = commonState.value.reduce()
        _commonState.value = newState
    }

    /**
     * 특정 Effect를 발생시킨다.
     */
    protected fun setEffect(builder: () -> Effect) {
        val effectValue = builder()
        val wrappedEffect = WrappedEffect(effectValue)
        scope.launch { _effect.send(wrappedEffect) }
    }

    /**
     * 특정 CommonEffect를 발생시킨다.
     */
    protected fun setCommonEffect(builder: () -> CommonEffect) {
        val effectValue = builder()
        val stampedEffect = WrappedEffect(effectValue)
        scope.launch { _commonEffect.send(stampedEffect) }
    }

    /**
     * State의 초기값을 정의한다.
     */
    abstract fun createInitialState(): State

    /**
     * CommonState의 초기값을 정의한다.
     */
    open fun createCommonState(): CommonState {
        return CommonState(
            isLoading = false,
            dialogConfig = null
        )
    }

    /**
     * handleEvent를 통해 정의된 Event를 구독한다.
     */
    private fun subscribeEvents() {
        scope.launch {
            event.collect {
                handleEvent(it)
            }
        }
    }

    /**
     * Loading 상태를 업데이트하는 함수
     */
    open fun setLoadingState(isLoading: Boolean) {
        setCommonState { copy(isLoading = isLoading) }
    }

    //region Common Effect Function정의
    /**
     * 문자열을 사용하여 Toast 메시지 표시 Effect를 발생시킨다.
     */
    open fun toast(message: String) {
        setCommonEffect { CommonEffect.Toast(message) }
    }

    /**
     * 리소스 ID를 사용하여 Toast 메시지 표시 Effect를 발생시킨다.
     */
    open fun toast(@StringRes resId: Int) {
        setCommonEffect { CommonEffect.ToastRes(resId) }
    }

    /**
     * 지정된 Route로 이동하기 위해 네비게이션 Effect를 발생시킨다.
     */
    open fun navigate(route: String, args: Map<String, Any?> = emptyMap(), navOptions: NavOptions? = null, extra: Navigator.Extras? = null) {
        setCommonEffect { CommonEffect.Navigate(route, navOptions, extra, args) }
    }

    /**
     * 이전 화면으로 돌아가는 Effect를 발생시킨다.
     */
    open fun backStack() {
        setCommonEffect { CommonEffect.BackStack }
    }

    /**
     * 지정된 Route까지의 백스택을 팝(Pop)하는 Effect를 발생시킨다.
     */
    open fun popUpTo(route: String, inclusive: Boolean = false) {
        setCommonEffect { CommonEffect.PopUpTo(route, inclusive) }
    }

    /**
     * 현재 Activity를 종료하는 Effect를 발생시킨다.
     */
    open fun finish() {
        setCommonEffect { CommonEffect.Finish }
    }

    /**
     * 현재 태스크를 정리하는 Effect를 발생시킨다.
     */
    open fun removeTask() {
        setCommonEffect { CommonEffect.RemoveTask }
    }

    /**
     * 프로세스를 강제 종료하는 Effect를 발생시킨다.
     */
    open fun exitProcess() {
        setCommonEffect { CommonEffect.ExitProcess }
    }

    /**
     * Activity를 재생성하는 Effect를 발생시킨다.
     */
    open fun recreate() {
        setCommonEffect { CommonEffect.Recreate }
    }

    /**
     * CommonState를 변경하여 다이얼로그를 화면에 노출한다.
     */
    open fun showDialog(config: DialogConfig) {
        setCommonState { copy(dialogConfig = config) }
    }

    /**
     * CommonState를 변경하여 다이얼로그를 화면에서 숨긴다.
     */
    open fun hideDialog() {
        setCommonState { copy(dialogConfig = null) }
    }

    /**
     * Intent를 사용하여 다른 Activity를 실행하는 Effect를 발생시킨다.
     */
    open fun startActivity(intent: Intent) {
        setCommonEffect { CommonEffect.StartActivity(intent) }
    }

    /**
     * 애플리케이션을 초기화하고 재시작하는 Effect를 발생시킨다.
     */
    open fun restart() {
        setCommonEffect { CommonEffect.Restart }
    }
    //endregion
}