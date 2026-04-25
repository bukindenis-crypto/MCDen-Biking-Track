package com.example.bikegoal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bikegoal.ui.theme.BikeGoalTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BikeGoalTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppWithSplash()
                }
            }
        }
    }
}

// ==================== SPLASH SCREEN ====================

@Composable
fun AppWithSplash() {
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2500)
        showSplash = false
    }

    AnimatedContent(
        targetState = showSplash,
        transitionSpec = {
            fadeIn(animationSpec = tween(800)) togetherWith 
            fadeOut(animationSpec = tween(600))
        },
        label = "splash_transition"
    ) { isSplash ->
        if (isSplash) {
            SplashScreen()
        } else {
            BikeGoalApp()
        }
    }
}

@Composable
fun SplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "bike_animation")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = CubicBezierEasing(0.65f, 0.0f, 0.35f, 1.0f)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val wheelAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wheel_alpha"
    )

    val textAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1000, delayMillis = 500),
        label = "text_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A237E),
                        Color(0xFF283593),
                        Color(0xFF3949AB),
                        Color(0xFF5C6BC0)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .scale(scale),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsBike,
                    contentDescription = "Шоссейный велосипед",
                    modifier = Modifier.size(140.dp),
                    tint = Color.White.copy(alpha = wheelAlpha)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { this.alpha = textAlpha }
            ) {
                Text(
                    text = "MCDen",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 4.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Biking Track",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                    color = Color(0xFFBBDEFB),
                    letterSpacing = 6.sp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            LoadingBar()
        }

        Text(
            text = "v1.0",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}

@Composable
fun LoadingBar() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loading_progress"
    )

    Box(
        modifier = Modifier
            .width(200.dp)
            .height(4.dp)
            .background(Color.White.copy(alpha = 0.2f), shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF64B5F6),
                            Color(0xFF90CAF9),
                            Color.White
                        )
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                )
        )
    }
}

// ==================== MAIN APP ====================

data class RideRecord(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val kilometers: Double,
    val timestamp: Long = System.currentTimeMillis()
)

data class MonthlyGoal(
    val monthYear: String,
    val targetKm: Double
)

class BikeDataStore(context: android.content.Context) {
    private val prefs = context.getSharedPreferences("bike_goal_prefs", android.content.Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getCurrentMonthKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return sdf.format(Date())
    }

    fun saveGoal(km: Double) {
        val goal = MonthlyGoal(getCurrentMonthKey(), km)
        prefs.edit().putString("goal_${getCurrentMonthKey()}", gson.toJson(goal)).apply()
    }

    fun getGoal(): Double {
        val json = prefs.getString("goal_${getCurrentMonthKey()}", null) ?: return 0.0
        val goal = gson.fromJson(json, MonthlyGoal::class.java)
        return if (goal.monthYear == getCurrentMonthKey()) goal.targetKm else 0.0
    }

    fun addRide(km: Double) {
        val rides = getRides().toMutableList()
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        rides.add(RideRecord(date = sdf.format(Date()), kilometers = km))
        prefs.edit().putString("rides_${getCurrentMonthKey()}", gson.toJson(rides)).apply()
    }

    fun getRides(): List<RideRecord> {
        val json = prefs.getString("rides_${getCurrentMonthKey()}", null) ?: return emptyList()
        val type = object : TypeToken<List<RideRecord>>() {}.type
        val rides = gson.fromJson<List<RideRecord>>(json, type) ?: return emptyList()
        val currentMonth = getCurrentMonthKey()
        return rides.filter {
            val rideMonth = it.date.substringAfterLast(".").take(7).replace(".", "-")
            rideMonth == currentMonth || it.date.length < 10
        }
    }

    fun deleteRide(id: String) {
        val rides = getRides().toMutableList().filter { it.id != id }
        prefs.edit().putString("rides_${getCurrentMonthKey()}", gson.toJson(rides)).apply()
    }

    fun getTotalRidden(): Double {
        return getRides().sumOf { it.kilometers }
    }

    fun getRemaining(): Double {
        val goal = getGoal()
        val ridden = getTotalRidden()
        return max(0.0, goal - ridden)
    }

    fun getProgress(): Float {
        val goal = getGoal()
        if (goal == 0.0) return 0f
        return (getTotalRidden() / goal).toFloat().coerceIn(0f, 1f)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BikeGoalApp() {
    val context = LocalContext.current
    val dataStore = remember { BikeDataStore(context) }

    var goal by remember { mutableStateOf(dataStore.getGoal()) }
    var showGoalDialog by remember { mutableStateOf(goal == 0.0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val totalRidden = remember(goal, showAddDialog, showGoalDialog, showResetDialog) {
        dataStore.getTotalRidden()
    }
    val remaining = remember(goal, showAddDialog, showGoalDialog, showResetDialog) {
        dataStore.getRemaining()
    }
    val progress = remember(goal, showAddDialog, showGoalDialog, showResetDialog) {
        dataStore.getProgress()
    }
    val rides = remember(goal, showAddDialog, showGoalDialog, showResetDialog) {
        dataStore.getRides().sortedByDescending { it.timestamp }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBike,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            "MCDen Biking Track",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = { showGoalDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Изменить цель")
                    }
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Сбросить")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить поездку")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ProgressCard(
                    goal = goal,
                    totalRidden = totalRidden,
                    remaining = remaining,
                    progress = progress
                )
            }

            item {
                StatsRow(
                    totalRidden = totalRidden,
                    remaining = remaining,
                    ridesCount = rides.size
                )
            }

            item {
                Text(
                    text = "История поездок",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (rides.isEmpty()) {
                item {
                    EmptyState()
                }
            } else {
                items(rides) { ride ->
                    RideItem(
                        ride = ride,
                        onDelete = {
                            dataStore.deleteRide(ride.id)
                            goal = dataStore.getGoal()
                        }
                    )
                }
            }
        }
    }

    if (showGoalDialog) {
        GoalDialog(
            currentGoal = goal,
            onDismiss = { showGoalDialog = false },
            onConfirm = { newGoal ->
                dataStore.saveGoal(newGoal)
                goal = newGoal
                showGoalDialog = false
            }
        )
    }

    if (showAddDialog) {
        AddRideDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { km ->
                dataStore.addRide(km)
                showAddDialog = false
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Сбросить данные?") },
            text = { Text("Все поездки за текущий месяц будут удалены. Цель останется.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val currentGoal = goal
                        context.getSharedPreferences("bike_goal_prefs", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .remove("rides_${dataStore.getCurrentMonthKey()}")
                            .apply()
                        goal = currentGoal
                        showResetDialog = false
                    }
                ) {
                    Text("Сбросить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun ProgressCard(goal: Double, totalRidden: Double, remaining: Double, progress: Float) {
    val isGoalReached = progress >= 1f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGoalReached) 
                MaterialTheme.colorScheme.tertiaryContainer 
            else 
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (isGoalReached) 
                    MaterialTheme.colorScheme.tertiary 
                else 
                    MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (goal == 0.0) {
                Text(
                    "Цель не установлена",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = if (isGoalReached) "🎉 Цель достигнута!" else "В пути к цели",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    color = if (isGoalReached) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "${String.format("%.1f", totalRidden)} / ${String.format("%.1f", goal)} км",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                if (!isGoalReached) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Осталось: ${String.format("%.1f", remaining)} км",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isGoalReached) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun StatsRow(totalRidden: Double, remaining: Double, ridesCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            title = "Проехано",
            value = "${String.format("%.1f", totalRidden)} км",
            icon = Icons.Default.DirectionsBike,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Поездок",
            value = ridesCount.toString(),
            icon = Icons.Default.Flag,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Осталось",
            value = "${String.format("%.1f", remaining)} км",
            icon = Icons.Default.Flag,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RideItem(ride: RideRecord, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsBike,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "${String.format("%.1f", ride.kilometers)} км",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = ride.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.DirectionsBike,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Пока нет поездок",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Нажмите + чтобы добавить первую",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun GoalDialog(currentGoal: Double, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var text by remember { mutableStateOf(if (currentGoal > 0) currentGoal.toString() else "") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Установить цель на месяц") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { 
                    text = it
                    error = false
                },
                label = { Text("Километров в месяц") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = error,
                supportingText = {
                    if (error) {
                        Text(
                            "Введите число больше 0",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cleanText = text.replace(",", ".").trim()
                    val km = cleanText.toDoubleOrNull()
                    
                    if (km != null && km > 0) {
                        onConfirm(km)
                    } else {
                        error = true
                    }
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun AddRideDialog(onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить поездку") },
        text = {
            Column {
                Text(
                    "Сколько километров вы проехали сегодня?",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { 
                        text = it
                        error = false
                    },
                    label = { Text("Километров") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = error,
                    supportingText = {
                        if (error) {
                            Text(
                                "Введите число больше 0",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cleanText = text.replace(",", ".").trim()
                    val km = cleanText.toDoubleOrNull()
                    
                    if (km != null && km > 0) {
                        onConfirm(km)
                    } else {
                        error = true
                    }
                }
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
