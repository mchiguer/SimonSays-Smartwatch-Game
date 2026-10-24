package com.epfl.esl.simongame.data

data class Strings(
    // General
    val back: String,
    val settings: String,
    val quit: String,
    val start: String,
    val continue_: String,
    val retry: String,
    val playAgain: String,

    // Game phases
    val simonSays: String,
    val doThisMove: String,
    val go: String,
    val analyzing: String,
    val great: String,
    val missed: String,
    val trap: String,
    val iDidntSay: String,
    val gameOver: String,
    val tryAgain: String,
    val movementUnclear: String,
    val skipping: String,

    // Moves
    val moveJump: String,
    val moveRaiseArms: String,
    val moveSpinAround: String,
    val moveClap: String,
    val moveSayHi: String,
    val moveRunInPlace: String,

    // Stats
    val round: String,
    val score: String,
    val combo: String,
    val noCombo: String,
    val lives: String,
    val solo: String,

    // Results
    val point: String,
    val points: String,
    val minusPoint: String,
    val trophyPlus: String,
    val livesRemaining: String,

    // Settings
    val language: String,
    val sound: String,
    val vibration: String,
    val comingSoon: String,

    // Trap message
    val simonSaysExclaim: String,

    // Retry
    val attemptOf: String,  // "Attempt X/2" format

    // Idle state
    val goodJob: String,
    val youDidntMove: String
)

object AppStrings {
    private val english = Strings(
        back = "< BACK",
        settings = "SETTINGS",
        quit = "QUIT",
        start = "START",
        continue_ = "CONTINUE",
        retry = "RETRY",
        playAgain = "PLAY AGAIN",

        simonSays = "SIMON SAYS",
        doThisMove = "DO THIS MOVE:",
        go = "GO!",
        analyzing = "ANALYZING...",
        great = "GREAT!",
        missed = "MISSED!",
        trap = "TRAP!",
        iDidntSay = "I didn't say",
        gameOver = "GAME OVER",
        tryAgain = "TRY AGAIN!",
        movementUnclear = "Movement unclear",
        skipping = "Skipping...",

        moveJump = "JUMP",
        moveRaiseArms = "RAISE ARMS",
        moveSpinAround = "SPIN AROUND",
        moveClap = "CLAP",
        moveSayHi = "SAY HI",
        moveRunInPlace = "RUN IN PLACE",

        round = "ROUND",
        score = "SCORE",
        combo = "COMBO",
        noCombo = "NO",
        lives = "LIVES",
        solo = "SOLO",

        point = "POINT",
        points = "POINTS",
        minusPoint = "-1 POINT",
        trophyPlus = "TROPHY +1",
        livesRemaining = "LIVES REMAINING:",

        language = "LANGUAGE",
        sound = "SOUND",
        vibration = "VIBRATION",
        comingSoon = "COMING SOON",

        simonSaysExclaim = "SIMON SAYS!",
        attemptOf = "Attempt %d/2",
        goodJob = "GOOD JOB!",
        youDidntMove = "You didn't move!"
    )

    private val french = Strings(
        back = "< RETOUR",
        settings = "OPTIONS",
        quit = "QUITTER",
        start = "JOUER",
        continue_ = "CONTINUER",
        retry = "REJOUER",
        playAgain = "REJOUER",

        simonSays = "SIMON DIT",
        doThisMove = "FAIS CE GESTE:",
        go = "GO!",
        analyzing = "ANALYSE...",
        great = "BRAVO!",
        missed = "RATE!",
        trap = "PIEGE!",
        iDidntSay = "Je n'ai pas dit",
        gameOver = "GAME OVER",
        tryAgain = "REESSAIE!",
        movementUnclear = "Mouvement flou",
        skipping = "On passe...",

        moveJump = "SAUTER",
        moveRaiseArms = "LEVER BRAS",
        moveSpinAround = "TOURNER",
        moveClap = "APPLAUDIR",
        moveSayHi = "SALUER",
        moveRunInPlace = "COURIR",

        round = "TOUR",
        score = "SCORE",
        combo = "COMBO",
        noCombo = "PAS DE",
        lives = "VIES",
        solo = "SOLO",

        point = "POINT",
        points = "POINTS",
        minusPoint = "-1 POINT",
        trophyPlus = "TROPHEE +1",
        livesRemaining = "VIES RESTANTES:",

        language = "LANGUE",
        sound = "SON",
        vibration = "VIBRATION",
        comingSoon = "BIENTOT",

        simonSaysExclaim = "SIMON DIT!",
        attemptOf = "Essai %d/2",
        goodJob = "BIEN JOUE!",
        youDidntMove = "Tu n'as pas bouge!"
    )

    private val spanish = Strings(
        back = "< ATRAS",
        settings = "AJUSTES",
        quit = "SALIR",
        start = "JUGAR",
        continue_ = "CONTINUAR",
        retry = "REINTENTAR",
        playAgain = "JUGAR DE NUEVO",

        simonSays = "SIMON DICE",
        doThisMove = "HAZ ESTE GESTO:",
        go = "YA!",
        analyzing = "ANALIZANDO...",
        great = "GENIAL!",
        missed = "FALLASTE!",
        trap = "TRAMPA!",
        iDidntSay = "No dije",
        gameOver = "FIN DEL JUEGO",
        tryAgain = "OTRA VEZ!",
        movementUnclear = "Movimiento confuso",
        skipping = "Saltando...",

        moveJump = "SALTAR",
        moveRaiseArms = "LEVANTAR BRAZOS",
        moveSpinAround = "GIRAR",
        moveClap = "APLAUDIR",
        moveSayHi = "SALUDAR",
        moveRunInPlace = "CORRER",

        round = "RONDA",
        score = "PUNTOS",
        combo = "COMBO",
        noCombo = "SIN",
        lives = "VIDAS",
        solo = "SOLO",

        point = "PUNTO",
        points = "PUNTOS",
        minusPoint = "-1 PUNTO",
        trophyPlus = "TROFEO +1",
        livesRemaining = "VIDAS RESTANTES:",

        language = "IDIOMA",
        sound = "SONIDO",
        vibration = "VIBRACION",
        comingSoon = "PRONTO",

        simonSaysExclaim = "SIMON DICE!",
        attemptOf = "Intento %d/2",
        goodJob = "BUEN TRABAJO!",
        youDidntMove = "No te moviste!"
    )

    fun get(language: AppLanguage): Strings = when (language) {
        AppLanguage.ENGLISH -> english
        AppLanguage.FRENCH -> french
        AppLanguage.SPANISH -> spanish
    }
}

fun Strings.getMoveName(moveName: String): String = when (moveName) {
    "JUMP" -> moveJump
    "RAISE_ARMS" -> moveRaiseArms
    "SPIN_AROUND" -> moveSpinAround
    "CLAP" -> moveClap
    "SAY_HI" -> moveSayHi
    "RUN_IN_PLACE" -> moveRunInPlace
    else -> moveName.replace("_", " ")
}