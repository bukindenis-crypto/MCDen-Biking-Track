@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BikeGoalApp() {
    val context = LocalContext.current
    val dataStore = remember { BikeDataStore(context) }

    // Все данные — в mutableState, инициализируем при старте
    var goal by remember { mutableStateOf(dataStore.getGoal()) }
    var totalRidden by remember { mutableStateOf(dataStore.getTotalRidden()) }
    var remaining by remember { mutableStateOf(dataStore.getRemaining()) }
    var progress by remember { mutableStateOf(dataStore.getProgress()) }
    var rides by remember { mutableStateOf(dataStore.getRides().sortedByDescending { it.timestamp }) }

    var showGoalDialog by remember { mutableStateOf(goal == 0.0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Функция пересчёта всего состояния
    fun recalculate() {
        goal = dataStore.getGoal()
        totalRidden = dataStore.getTotalRidden()
        remaining = dataStore.getRemaining()
        progress = dataStore.getProgress()
        rides = dataStore.getRides().sortedByDescending { it.timestamp }
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
                items(
                    items = rides,
                    key = { it.id }
                ) { ride ->
                    RideItem(
                        ride = ride,
                        onDelete = {
                            dataStore.deleteRide(ride.id)
                            recalculate() // <-- Пересчёт
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
                recalculate() // <-- Пересчёт
                showGoalDialog = false
            }
        )
    }

    if (showAddDialog) {
        AddRideDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { km ->
                dataStore.addRide(km)
                recalculate() // <-- Пересчёт
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
                        context.getSharedPreferences("bike_goal_prefs", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .remove("rides_${dataStore.getCurrentMonthKey()}")
                            .apply()
                        recalculate() // <-- Пересчёт
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
