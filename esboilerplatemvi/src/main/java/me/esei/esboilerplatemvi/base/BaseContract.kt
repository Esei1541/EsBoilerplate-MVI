package me.esei.esboilerplatemvi.base

import android.content.Intent
import androidx.annotation.StringRes
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import me.esei.esboilerplatemvi.model.DialogConfig

/**
 * Contract Class외 공통 사항을 정의한다.
 */
abstract class BaseContract {

    /**
     * View에서 발생하는 사용자의 액션이나 시스템 이벤트를 정의하는 마커 인터페이스.
     */
    interface UiEvent

    /**
     * View의 렌더링에 필요한 상태 데이터를 정의하는 마커 인터페이스.
     */
    interface UiState

    /**
     * View에서 단발성으로 처리되어야 하는 사이드 이펙트를 정의하는 마커 인터페이스.
     */
    interface UiEffect

    /**
     * 공통으로 사용되는 상태(로딩 여부, 다이얼로그 노출 상태 등)를 정의한다.
     * * @param isLoading 전체 화면에 대한 로딩 상태 활성화 여부
     * @param dialogConfig 다이얼로그 구성을 위한 설정 객체. null일 경우 다이얼로그를 숨긴다.
     */
    data class CommonState(
        val isLoading: Boolean = false,
        val dialogConfig: DialogConfig? = null
    ): UiState

    /**
     * 공통적으로 사용되는 Side Effect를 사전 정의한다.
     * 해당 Effect에 대한 동작은 BaseViewModel과 BaseCompose에서 정의한다.
     */
    sealed class CommonEffect: UiEffect {

        /**
         * UI에 Toast를 출력하기 위한 Effect
         */
        data class Toast(val message: String): CommonEffect()

        /**
         * UI에 String Resource를 통해 Toast를 출력하기 위한 Effect
         */
        data class ToastRes(@StringRes val resId: Int): CommonEffect()

        /**
         * 지정된 Route로 네비게이션 이동을 처리하기 위한 Effect
         */
        data class Navigate(val route: String, val navOptions: NavOptions? = null, val extras: Navigator.Extras? = null, val args: Map<String, Any?> = emptyMap()): CommonEffect()

        /**
         * 특정 Route까지 백스택을 팝(Pop)하기 위한 Effect
         */
        data class PopUpTo(val route: String, val inclusive: Boolean = false): CommonEffect()

        /**
         * 이전 화면으로 백스택을 팝(Pop)하기 위한 Effect
         */
        data object BackStack: CommonEffect()

        /**
         * 현재 활성화된 Activity를 종료하기 위한 Effect
         */
        data object Finish: CommonEffect()

        /**
         * 현재 태스크를 제거하고 앱을 종료하기 위한 Effect
         */
        data object RemoveTask: CommonEffect()

        /**
         * Intent를 통해 다른 Activity를 실행하기 위한 Effect
         */
        data class StartActivity(val intent: Intent): CommonEffect()

        /**
         * 프로세스를 강제 종료하기 위한 Effect
         */
        data object ExitProcess: CommonEffect()

        /**
         * 현재 Activity를 재생성(Recreate)하기 위한 Effect
         */
        data object Recreate: CommonEffect()

        /**
         * 애플리케이션을 초기화하고 재시작하기 위한 Effect
         */
        data object Restart: CommonEffect()

    }

}