# EsBoilerplate-MVI

![Maven Central](https://img.shields.io/maven-central/v/io.github.esei1541/esboilerplate-mvi)
![License](https://img.shields.io/badge/license-MIT-blue.svg)

## 1. 개요
`EsBoilerplate-MVI`는 Android Jetpack Compose 환경에서 MVI (Model-View-Intent) 아키텍처를 신속하고 일관되게 구현할 수 있도록 보일러플레이트 코드를 제공합니다.

UI의 상태(State), 사용자 액션(Event), 단발성 효과(Effect) 등을 명확하게 분리하여 관리하고, 화면 이동과 로딩 상태, 다이얼로그 노출, 토스트, 안드로이드 생명주기 관련 동작 등 앱 개발 환경에서 반복적으로 발생하는 공통 처리 로직을 사전 정의합니다. 

## 2. Setup
app 단위의 `build.gradle.kts` (또는 `build.gradle`) 에 아래와 같이 의존성을 추가하여 사용합니다.

```kotlin
dependencies {
    implementation("io.github.esei1541:esboilerplate-mvi:0.0.1")
}
```

## 3. Usage

### BaseContract
각 화면에서 사용할 상태, 이벤트, 효과를 정의하기 위한 인터페이스를 제공합니다. `UiState`, `UiEvent`, `UiEffect` 인터페이스를 상속하여 화면별 Contract를 구성합니다.

* **`CommonState`**: 모든 화면에서 공통으로 가지는 상태(로딩 여부 `isLoading`, 다이얼로그 설정 `dialogConfig`)를 내장합니다.
* **`CommonEffect`**: ViewModel에서 호출할 수 있는 공통 사이드 이펙트(`Toast`, `StartActivity`, `PopUpTo`, `Finish` 등)가 사전 정의되어 있습니다.

```kotlin
// 사용 예시
class MainContract : BaseContract() {
    data class State(
        val userName: String = ""
    ) : UiState
    
    sealed class Event : UiEvent {
        data class OnNameChanged(val name: String) : Event()
        data object OnSubmitClicked : Event()
    }
    
    sealed class Effect : UiEffect {
        data object ShowSuccessAnimation : Effect()
    }
}
```

### BaseViewModel
Contract Class에서 정의한 사항을 바탕으로 비즈니스 로직을 처리하는 ViewModel의 추상 클래스입니다.

* **주요 프로퍼티**
    * `state`: UI 렌더링에 사용되는 최신 상태(State)를 구독할 수 있는 `StateFlow`입니다.
    * `effect`: UI에서 1회성으로 실행할 효과(Effect)를 수신하는 `Flow`입니다.
* **주요 함수**
    * `createInitialState()`: **(필수 구현)** 화면의 초기 상태를 정의합니다.
    * `handleEvent(event: Event)`: **(필수 구현)** 뷰에서 전달된 이벤트를 처리하는 로직을 작성합니다.
    * `setState { ... }`: 현재 상태를 복사하여 새로운 상태로 업데이트합니다.
    * `setEffect { ... }`: View로 1회성 이펙트를 전달합니다.
    * `setEvent(event)`: View에서 이벤트를 ViewModel로 전달할 때 호출합니다.
* **공통 편의 함수**
    * 로딩 및 다이얼로그 제어: `setLoadingState(Boolean)`, `showDialog(DialogConfig)`, `hideDialog()`
    * 공통 이펙트 발생: `toast()`, `backStack()`, `finish()` 등

```kotlin
// 사용 예시
class MainViewModel : BaseViewModel<MainContract.Event, MainContract.State, MainContract.Effect>() {

    override fun createInitialState() = MainContract.State()

    override fun handleEvent(event: MainContract.Event) {
        when (event) {
            is MainContract.Event.OnNameChanged -> {
                setState { copy(userName = event.name) }
            }
            is MainContract.Event.OnSubmitClicked -> {
                setLoadingState(true)
                // 비즈니스 로직 처리 후 이펙트 발생
                setEffect { MainContract.Effect.ShowSuccessAnimation }
                navigate(route = "next_screen")
            }
        }
    }
}
```

### BaseComposable
최상위 단위의 Composable에서 호출하여 UI 컴포넌트들을 감싸는 래퍼(Wrapper) 역할을 합니다. `BaseViewModel`과 자동으로 연동되어 공통 상태 및 생명주기를 관리합니다.

* **주요 패러미터**
    * `viewModel`: 화면과 연결된 `BaseViewModel` 인스턴스.
    * `navController`: 화면 이동을 처리할 `NavController`.
    * `loadingContent`: 뷰모델에서 `isLoading = true` 상태일 때 노출될 Composable 슬롯.
    * `dialogContent`: 뷰모델에서 `showDialog()` 호출 시 노출될 Composable 슬롯.
* **내부 동작**
    * ViewModel에서 발생한 `CommonEffect`(네비게이션 이동, 토스트 출력 등)를 `LaunchedEffect` 내에서 자동으로 처리합니다.
    * `LifecycleEventObserver`를 통해 Android 생명주기(Create, Start, Resume 등)를 감지하고 ViewModel의 훅(Hook) 메서드(`onLocalLifecycleCreate()` 등)를 호출합니다.
    * 시스템 뒤로 가기 동작을 인터셉트하여 뷰모델의 `onBackPressed()`로 전달합니다.

```kotlin
// 사용 예시
@Composable
fun MainScreen(viewModel: MainViewModel, navController: NavController) {
    val state by viewModel.state.collectAsState()

    BaseComposable(
        viewModel = viewModel,
        navController = navController,
        loadingContent = { CustomCircularProgressIndicator() }, // 공통 로딩 UI
        dialogContent = { config -> CustomDialog(config) }      // 공통 다이얼로그 UI
    ) {
        // 실제 화면 레이아웃 구성
        Column(modifier = Modifier.fillMaxSize()) {
            Text(text = "Hello, ${state.userName}")
            Button(onClick = { viewModel.setEvent(MainContract.Event.OnSubmitClicked) }) {
                Text("Submit")
            }
        }
    }
}
```

### 기타 유틸리티 모델
* **`WrappedEffect`**: `Channel`을 통해 Effect를 전달할 때 동일한 객체 내용이라도 누락 없이 실행되도록 고유한 `timeStamp`를 부여하는 래퍼 데이터 클래스입니다. 내부적으로 자동 적용됩니다.
* **`DialogConfig`**: 다이얼로그 UI를 띄울 때 필요한 설정값(제목, 내용, 버튼 텍스트 등)을 정의하기 위한 마커 인터페이스입니다. 프로젝트 요구사항에 맞게 이 인터페이스를 구현하는 데이터 클래스를 만들어 사용할 수 있습니다.