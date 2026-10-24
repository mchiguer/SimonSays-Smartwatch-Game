package com.epfl.esl.simongame.navigation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.epfl.esl.simongame.auth.AuthState
import com.epfl.esl.simongame.auth.AuthViewModel
import com.epfl.esl.simongame.data.GameSettings
import com.epfl.esl.simongame.data.UserRepository
import com.epfl.esl.simongame.di.LocalAppContainer
import com.epfl.esl.simongame.friends.FriendSearchViewModel
import com.epfl.esl.simongame.friends.FriendSearchViewModelFactory
import com.epfl.esl.simongame.friends.FriendsListViewModel
import com.epfl.esl.simongame.friends.FriendsListViewModelFactory
import com.epfl.esl.simongame.home.HomeViewModel
import com.epfl.esl.simongame.screens.FriendSearchScreen
import com.epfl.esl.simongame.screens.FriendsListScreen
import com.epfl.esl.simongame.screens.HomeScreen
import com.epfl.esl.simongame.screens.MultiGameScreen
import com.epfl.esl.simongame.screens.ProfileScreen
import com.epfl.esl.simongame.screens.RegisterScreen
import com.epfl.esl.simongame.screens.SettingsScreen
import com.epfl.esl.simongame.screens.SoloGameScreen
import com.epfl.esl.simongame.screens.SoloResultScreen
import com.epfl.esl.simongame.solo_mode.SoloGameViewModel
import com.epfl.esl.simongame.solo_mode.SoloGameViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer.current

    val authVm: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(container.userRepository)
    )
    val authState by authVm.state.collectAsState()
    val isLoading by authVm.isLoading.collectAsState()
    val errorMessage by authVm.errorMessage.collectAsState()

    when (authState) {
        AuthState.Loading -> {
        }

        AuthState.Unregistered -> {
            RegisterScreen(
                onRegister = { username, email, password, isLogin ->
                    authVm.authenticate(username, email, password, isLogin)
                },
                isLoading = isLoading,
                errorMessage = errorMessage
            )
        }

        AuthState.Registered -> {
            val homeVm: HomeViewModel = viewModel(
                factory = HomeViewModelFactory(container.userRepository)
            )
            val homeUi by homeVm.uiState.collectAsState()

            val currentUser by container.userRepository.currentUser.collectAsState(initial = null)

            NavHost(
                navController = navController,
                startDestination = AppDestinations.HOME,
                modifier = modifier
            ) {

                composable(AppDestinations.HOME) {
                    HomeScreen(
                        onPlaySoloClick = { navController.navigate(AppDestinations.SOLO_GAME) },
                        onPlayVersusClick = { navController.navigate(AppDestinations.MULTI_GAME) },
                        onProfileClick = { navController.navigate(AppDestinations.PROFILE) },
                        onSettingsClick = { navController.navigate(AppDestinations.SETTINGS) }
                    )
                }

                composable(AppDestinations.MULTI_GAME) {
                    val directoryUsers by container.userRepository.directoryUsers.collectAsState(initial = emptyList())
                    val friendIds = currentUser?.friends?.toSet() ?: emptySet()
                    val friends = directoryUsers.filter { it.id in friendIds }

                    MultiGameScreen(
                        onBack = { navController.popBackStack() },
                        currentUser = currentUser,
                        friends = friends
                    )
                }

                composable(AppDestinations.SOLO_GAME) {
                    val context = LocalContext.current
                    val soloVm: SoloGameViewModel = viewModel(
                        factory = SoloGameViewModelFactory(
                            container.motionToMovePredictor,
                            container.motionWindowRepository,
                            container.phoneWearClient,
                            context.applicationContext
                        )
                    )
                    val soloState by soloVm.uiState.collectAsState()
                    val gameSettings by container.settingsRepository.settings.collectAsState(
                        initial = GameSettings()
                    )

                    LaunchedEffect(soloState.gameResult) {
                        val r = soloState.gameResult ?: return@LaunchedEffect
                        homeVm.onSoloGameFinished(r)

                        navController.navigate(
                            AppDestinations.soloResultRoute(
                                score = r.finalScore,
                                maxScore = r.maxScore,
                                rounds = r.totalRounds,
                                mistakes = r.mistakes
                            )
                        )
                    }

                    SoloGameScreen(
                        uiState = soloState,
                        onBack = { navController.popBackStack() },
                        onStart = { soloVm.onStartPressed() },
                        onContinue = { soloVm.onContinuePressed() },
                        onRestart = { soloVm.onRestartPressed() },
                        onShakeComplete = { soloVm.onShakeComplete() },
                        onDone = { soloVm.onDonePressed() },
                        onTransitionToGameOver = { soloVm.onTransitionToGameOver() },
                        language = gameSettings.language
                    )
                }

                composable(
                    route = "${AppDestinations.CHALLENGE_GAME}/{challengeId}/{challengerName}/{scoreToBeat}",
                    arguments = listOf(
                        navArgument("challengeId") { defaultValue = "" },
                        navArgument("challengerName") { defaultValue = "" },
                        navArgument("scoreToBeat") { defaultValue = 0 }
                    )
                ) { backStackEntry ->
                    val challengeId = backStackEntry.arguments?.getString("challengeId") ?: ""
                    val challengerName = backStackEntry.arguments?.getString("challengerName") ?: ""
                    val scoreToBeat = backStackEntry.arguments?.getInt("scoreToBeat") ?: 0

                    val challengeContext = LocalContext.current
                    val soloVm: SoloGameViewModel = viewModel(
                        factory = SoloGameViewModelFactory(
                            container.motionToMovePredictor,
                            container.motionWindowRepository,
                            container.phoneWearClient,
                            challengeContext.applicationContext
                        )
                    )
                    val soloState by soloVm.uiState.collectAsState()
                    val scope = rememberCoroutineScope()
                    val challengeSettings by container.settingsRepository.settings.collectAsState(
                        initial = GameSettings()
                    )

                    LaunchedEffect(soloState.gameResult) {
                        val r = soloState.gameResult ?: return@LaunchedEffect
                        homeVm.onSoloGameFinished(r)

                        scope.launch {
                            container.userRepository.completeChallenge(challengeId, r.finalScore)
                        }

                        navController.navigate(
                            AppDestinations.challengeResultRoute(
                                score = r.finalScore,
                                maxScore = r.maxScore,
                                rounds = r.totalRounds,
                                mistakes = r.mistakes,
                                challengeId = challengeId,
                                challengerName = challengerName,
                                scoreToBeat = scoreToBeat
                            )
                        )
                    }

                    SoloGameScreen(
                        uiState = soloState,
                        onBack = { navController.popBackStack() },
                        onStart = { soloVm.onStartPressed() },
                        onContinue = { soloVm.onContinuePressed() },
                        onRestart = { soloVm.onRestartPressed() },
                        onShakeComplete = { soloVm.onShakeComplete() },
                        onDone = { soloVm.onDonePressed() },
                        onTransitionToGameOver = { soloVm.onTransitionToGameOver() },
                        challengeMode = true,
                        challengerName = challengerName,
                        scoreToBeat = scoreToBeat,
                        language = challengeSettings.language
                    )
                }

                composable(AppDestinations.FRIEND_SEARCH) {
                    val container = LocalAppContainer.current

                    val vm: FriendSearchViewModel = viewModel(
                        factory = FriendSearchViewModelFactory(container.userRepository)
                    )
                    val ui by vm.uiState.collectAsState()

                    FriendSearchScreen(
                        ui = ui,
                        onBack = { navController.popBackStack() },
                        onQueryChange = vm::onQueryChange,
                        onAddFriend = vm::addFriend,
                        onDismissSuggestion = vm::dismissSuggestion
                    )
                }


                composable(
                    route = "${AppDestinations.SOLO_RESULT}/{score}/{maxScore}/{rounds}/{mistakes}/{isChallenge}/{challengerName}/{scoreToBeat}",
                    arguments = listOf(
                        navArgument("score") { defaultValue = 0 },
                        navArgument("maxScore") { defaultValue = 0 },
                        navArgument("rounds") { defaultValue = 0 },
                        navArgument("mistakes") { defaultValue = 0 },
                        navArgument("isChallenge") { defaultValue = 0 },
                        navArgument("challengerName") { defaultValue = "none" },
                        navArgument("scoreToBeat") { defaultValue = 0 }
                    )
                ) { backStackEntry ->
                    val score = backStackEntry.arguments?.getInt("score") ?: 0
                    val maxScore = backStackEntry.arguments?.getInt("maxScore") ?: score
                    val rounds = backStackEntry.arguments?.getInt("rounds") ?: 0
                    val mistakes = backStackEntry.arguments?.getInt("mistakes") ?: 0
                    val isChallenge = (backStackEntry.arguments?.getInt("isChallenge") ?: 0) == 1
                    val challengerName = backStackEntry.arguments?.getString("challengerName") ?: ""
                    val scoreToBeat = backStackEntry.arguments?.getInt("scoreToBeat") ?: 0

                    SoloResultScreen(
                        score = score,
                        maxScore = maxScore,
                        rounds = rounds,
                        mistakes = mistakes,
                        isChallenge = isChallenge,
                        challengerName = challengerName,
                        scoreToBeat = scoreToBeat,
                        onPlayAgain = {
                            navController.navigate(AppDestinations.SOLO_GAME) {
                                popUpTo(AppDestinations.HOME) { inclusive = false }
                            }
                        },
                        onBackHome = {
                            navController.popBackStack(AppDestinations.HOME, inclusive = false)
                        }
                    )
                }

                composable(AppDestinations.PROFILE) {
                    val profileContext = LocalContext.current
                    val profileScope = rememberCoroutineScope()
                    var showDeleteConfirmation by remember { mutableStateOf(false) }

                    val imagePickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        uri?.let { selectedUri ->
                            profileScope.launch {
                                try {
                                    val inputStream = profileContext.contentResolver.openInputStream(selectedUri)
                                    val imageBytes = inputStream?.readBytes()
                                    inputStream?.close()

                                    if (imageBytes != null) {
                                        container.userRepository.uploadProfilePicture(imageBytes)
                                    }
                                } catch (e: Exception) {
                                }
                            }
                        }
                    }

                    if (showDeleteConfirmation) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showDeleteConfirmation = false },
                            title = {
                                androidx.compose.material3.Text(
                                    text = "DELETE ACCOUNT?",
                                    fontFamily = com.epfl.esl.simongame.screens.PressStart2P,
                                    fontSize = 12.sp
                                )
                            },
                            text = {
                                androidx.compose.material3.Text(
                                    text = "This will permanently delete your account and all data. This cannot be undone.",
                                    fontFamily = com.epfl.esl.simongame.screens.PressStart2P,
                                    fontSize = 8.sp
                                )
                            },
                            confirmButton = {
                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        showDeleteConfirmation = false
                                        profileScope.launch {
                                            val result = container.userRepository.deleteAccount()
                                            if (result.isSuccess) {
                                                navController.navigate(AppDestinations.REGISTER) {
                                                    popUpTo(0) { inclusive = true }
                                                }
                                            }
                                        }
                                    }
                                ) {
                                    androidx.compose.material3.Text(
                                        "DELETE",
                                        color = androidx.compose.ui.graphics.Color(0xFFFF6B6B)
                                    )
                                }
                            },
                            dismissButton = {
                                androidx.compose.material3.TextButton(
                                    onClick = { showDeleteConfirmation = false }
                                ) {
                                    androidx.compose.material3.Text("CANCEL")
                                }
                            }
                        )
                    }

                    ProfileScreen(
                        username = currentUser?.username ?: "Player",
                        soloStats = homeUi,
                        totalTrophies = currentUser?.totalTrophies ?: 0,
                        profilePictureUrl = currentUser?.profilePictureUrl,
                        onBack = { navController.popBackStack() },
                        onFindFriends = { navController.navigate(AppDestinations.FRIEND_SEARCH) },
                        onLogout = { authVm.logout() },
                        onFriendsList = { navController.navigate(AppDestinations.FRIENDS_LIST) },
                        onChangePhoto = { imagePickerLauncher.launch("image/*") },
                        onRemovePhoto = {
                            profileScope.launch {
                                container.userRepository.removeProfilePicture()
                            }
                        },
                        onDeleteAccount = { showDeleteConfirmation = true }
                    )
                }

                composable(AppDestinations.FRIENDS_LIST) {
                    val container = LocalAppContainer.current

                    val vm: FriendsListViewModel = viewModel(
                        factory = FriendsListViewModelFactory(container.userRepository)
                    )
                    val ui by vm.uiState.collectAsState()

                    FriendsListScreen(
                        ui = ui,
                        onBack = { navController.popBackStack() },
                        onChallengeFriend = { friendId, friendName, myScore ->
                            vm.sendChallenge(friendId, friendName, myScore)
                        },
                        onAcceptChallenge = { challengeId ->
                            vm.acceptChallenge(challengeId)
                        },
                        onDeclineChallenge = { challengeId ->
                            vm.declineChallenge(challengeId)
                        },
                        onPlayChallenge = { challengeId, challengerName, scoreToBeat ->
                            navController.navigate(
                                AppDestinations.challengeGameRoute(challengeId, challengerName, scoreToBeat)
                            )
                        },
                        onRemoveFriend = { friendId ->
                            vm.removeFriend(friendId)
                        }
                    )
                }

                composable(AppDestinations.SETTINGS) {
                    val settingsState by container.settingsRepository.settings.collectAsState(
                        initial = GameSettings()
                    )
                    val scope = rememberCoroutineScope()

                    SettingsScreen(
                        settings = settingsState,
                        onBack = { navController.popBackStack() },
                        onLanguageSelected = { lang ->
                            scope.launch {
                                container.settingsRepository.setLanguage(lang)
                            }
                        }
                    )
                }

            }
        }
    }
}

private class AuthViewModelFactory(
    private val repo: UserRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}

private class HomeViewModelFactory(
    private val repo: UserRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}