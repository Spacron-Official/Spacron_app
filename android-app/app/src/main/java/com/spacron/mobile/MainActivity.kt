package com.spacron.mobile

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

data class ApiEnvelope<T>(val success: Boolean = false, val data: T? = null, val error: String? = null)
data class SessionUser(
    val id: String,
    val email: String,
    val name: String,
    val role: String,
    val skills: List<String> = emptyList(),
    val balance: Double = 0.0,
    val tasksCompleted: Int = 0,
    val rating: Double = 0.0,
    val status: String = "active",
    val joinedAt: String = "",
    val avatar: String = "?",
)
data class AuthPayload(val token: String, val user: SessionUser)
data class TaskSubmission(val text: String = "", val link: String = "", val file: String? = null)
data class TaskItem(
    val id: Long,
    val title: String,
    val price: Double,
    val status: String,
    val assignedTo: String? = null,
    val description: String = "",
    val timeEstimateMinutes: Int = 60,
    val difficulty: String = "Medium",
    val category: String = "General",
    val postedBy: String = "",
    val sellerId: Long? = null,
    val createdAt: String = "",
    val commissionRate: Double = 0.15,
    val commission: Double = 0.0,
    val userEarning: Double = 0.0,
    val adminRevenue: Double = 0.0,
    val contactInfo: String = "",
    val requiresContact: Boolean = false,
    val submission: TaskSubmission? = null,
)
data class ListingPayment(val id: String, val taskId: Long, val amount: Double, val sellerEmail: String, val type: String, val createdAt: String)
data class FinancePayload(val platformWalletBalance: Double, val listingPayments: List<ListingPayment>, val commissionFromTasks: Double)
data class StatsPayload(val userCount: Int, val taskCount: Int, val completedCount: Int, val paidOutTotal: Double)
data class ConfigPayload(val commissionPct: Int)
data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val email: String, val name: String, val password: String, val role: String, val skills: List<String> = emptyList())
data class CreateTaskRequest(
    val title: String,
    val price: Double,
    val description: String,
    val timeEstimateMinutes: Int,
    val difficulty: String,
    val category: String,
    val postedBy: String,
    val sellerId: Long,
    val contactInfo: String,
    val requiresContact: Boolean,
    val commissionRate: Double,
)
data class AcceptTaskRequest(val assignedTo: String)
data class SubmitTaskRequest(val text: String, val link: String, val fileName: String? = null, val fileBase64: String? = null)
data class UpdateStatusRequest(val status: String)
data class CommissionRequest(val commissionPct: Int)

interface SpacronApi {
    @POST("api/login")
    suspend fun login(@Body body: LoginRequest): ApiEnvelope<AuthPayload>

    @POST("api/register")
    suspend fun register(@Body body: RegisterRequest): ApiEnvelope<AuthPayload>

    @GET("api/tasks")
    suspend fun tasks(@Header("Authorization") auth: String? = null): ApiEnvelope<List<TaskItem>>

    @POST("api/tasks")
    suspend fun createTask(@Header("Authorization") auth: String, @Body body: CreateTaskRequest): ApiEnvelope<TaskItem>

    @POST("api/tasks/{id}/pay")
    suspend fun payTask(@Header("Authorization") auth: String, @Path("id") id: Long): ApiEnvelope<TaskItem>

    @POST("api/tasks/{id}/accept")
    suspend fun acceptTask(@Header("Authorization") auth: String, @Path("id") id: Long, @Body body: AcceptTaskRequest): ApiEnvelope<TaskItem>

    @POST("api/tasks/{id}/submit")
    suspend fun submitTask(@Header("Authorization") auth: String, @Path("id") id: Long, @Body body: SubmitTaskRequest): ApiEnvelope<TaskItem>

    @POST("api/tasks/{id}/approve")
    suspend fun approveTask(@Header("Authorization") auth: String, @Path("id") id: Long): ApiEnvelope<TaskItem>

    @POST("api/tasks/{id}/reject")
    suspend fun rejectTask(@Header("Authorization") auth: String, @Path("id") id: Long): ApiEnvelope<TaskItem>

    @GET("api/config")
    suspend fun config(): ApiEnvelope<ConfigPayload>

    @POST("api/config")
    suspend fun updateConfig(@Header("Authorization") auth: String, @Body body: CommissionRequest): ApiEnvelope<ConfigPayload>

    @GET("api/users")
    suspend fun users(@Header("Authorization") auth: String): ApiEnvelope<List<SessionUser>>

    @PATCH("api/users/{id}")
    suspend fun updateUserStatus(@Header("Authorization") auth: String, @Path("id") id: Long, @Body body: UpdateStatusRequest): ApiEnvelope<SessionUser>

    @GET("api/admin/finance")
    suspend fun finance(@Header("Authorization") auth: String): ApiEnvelope<FinancePayload>

    @GET("api/stats")
    suspend fun stats(): ApiEnvelope<StatsPayload>
}

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("spacron_session", Context.MODE_PRIVATE)

    fun save(token: String, user: SessionUser) {
        prefs.edit()
            .putString("token", token)
            .putString("user_id", user.id)
            .putString("user_email", user.email)
            .putString("user_name", user.name)
            .putString("user_role", user.role)
            .putFloat("user_balance", user.balance.toFloat())
            .apply()
    }

    fun restore(): Pair<String, SessionUser>? {
        val token = prefs.getString("token", null) ?: return null
        val userId = prefs.getString("user_id", null) ?: return null
        val role = prefs.getString("user_role", null) ?: return null
        return token to SessionUser(
            id = userId,
            email = prefs.getString("user_email", "") ?: "",
            name = prefs.getString("user_name", "") ?: "",
            role = role,
            balance = prefs.getFloat("user_balance", 0f).toDouble(),
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}

class MainViewModel(private val store: SessionStore) : ViewModel() {
    private val api: SpacronApi
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .addInterceptor(Interceptor { chain -> chain.proceed(chain.request()) })
            .build()
        api = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()
            .create(SpacronApi::class.java)
        store.restore()?.let { (token, user) ->
            _ui.value = _ui.value.copy(token = token, currentUser = user)
        }
        refresh()
    }

    fun refresh() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            runCatching {
                val auth = authHeader()
                val tasksResponse = api.tasks(auth).data.orEmpty()
                val commission = api.config().data?.commissionPct ?: 15
                val stats = api.stats().data
                val finance = if (_ui.value.currentUser?.role == "admin" && auth != null) api.finance(auth).data else null
                val users = if (_ui.value.currentUser?.role == "admin" && auth != null) api.users(auth).data.orEmpty() else emptyList()
                _ui.value = _ui.value.copy(
                    tasks = tasksResponse,
                    commissionPct = commission,
                    stats = stats,
                    finance = finance,
                    users = users,
                    error = null,
                )
            }.onFailure {
                _ui.value = _ui.value.copy(error = it.message)
            }
        }
    }

    fun login(email: String, password: String) {
        executeMessage {
            val response = api.login(LoginRequest(email.trim(), password))
            val payload = response.data ?: error(response.error ?: "Unable to login")
            store.save(payload.token, payload.user)
            _ui.value = _ui.value.copy(token = payload.token, currentUser = payload.user)
            refresh()
            "Signed in as ${payload.user.name}"
        }
    }

    fun register(name: String, email: String, password: String, role: String, skills: List<String>) {
        executeMessage {
            val response = api.register(RegisterRequest(email.trim(), name.trim(), password, role, skills))
            val payload = response.data ?: error(response.error ?: "Unable to register")
            store.save(payload.token, payload.user)
            _ui.value = _ui.value.copy(token = payload.token, currentUser = payload.user)
            refresh()
            "Account created"
        }
    }

    fun logout() {
        store.clear()
        _ui.value = UiState()
        refresh()
    }

    fun createTask(form: CreateTaskRequest) {
        executeMessage {
            val auth = requireNotNull(authHeader()) { "Authentication required" }
            val response = api.createTask(auth, form)
            if (!response.success) error(response.error ?: "Unable to create task")
            refresh()
            "Task created"
        }
    }

    fun payTask(taskId: Long) {
        executeMessage {
            val auth = requireNotNull(authHeader()) { "Authentication required" }
            val response = api.payTask(auth, taskId)
            if (!response.success) error(response.error ?: "Unable to publish task")
            refresh()
            "Task published"
        }
    }

    fun acceptTask(taskId: Long) {
        val email = _ui.value.currentUser?.email ?: return
        executeMessage {
            val auth = requireNotNull(authHeader()) { "Authentication required" }
            val response = api.acceptTask(auth, taskId, AcceptTaskRequest(email))
            if (!response.success) error(response.error ?: "Unable to accept task")
            refresh()
            "Task accepted"
        }
    }

    fun submitTask(taskId: Long, text: String, link: String) {
        executeMessage {
            val auth = requireNotNull(authHeader()) { "Authentication required" }
            val response = api.submitTask(auth, taskId, SubmitTaskRequest(text = text, link = link))
            if (!response.success) error(response.error ?: "Unable to submit task")
            refresh()
            "Work submitted"
        }
    }

    fun approveTask(taskId: Long) {
        executeMessage {
            val auth = requireNotNull(authHeader()) { "Authentication required" }
            val response = api.approveTask(auth, taskId)
            if (!response.success) error(response.error ?: "Unable to approve task")
            refresh()
            "Task approved"
        }
    }

    fun rejectTask(taskId: Long) {
        executeMessage {
            val auth = requireNotNull(authHeader()) { "Authentication required" }
            val response = api.rejectTask(auth, taskId)
            if (!response.success) error(response.error ?: "Unable to reject task")
            refresh()
            "Task rejected"
        }
    }

    fun updateCommission(value: Int) {
        executeMessage {
            val auth = requireNotNull(authHeader()) { "Authentication required" }
            val response = api.updateConfig(auth, CommissionRequest(value))
            if (!response.success) error(response.error ?: "Unable to update commission")
            refresh()
            "Commission updated"
        }
    }

    fun updateUserStatus(id: Long, status: String) {
        executeMessage {
            val auth = requireNotNull(authHeader()) { "Authentication required" }
            val response = api.updateUserStatus(auth, id, UpdateStatusRequest(status))
            if (!response.success) error(response.error ?: "Unable to update user")
            refresh()
            "User updated"
        }
    }

    private fun executeMessage(block: suspend () -> String) {
        _ui.value = _ui.value.copy(isBusy = true, error = null, message = null)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            runCatching { block() }
                .onSuccess { _ui.value = _ui.value.copy(isBusy = false, message = it, error = null) }
                .onFailure { _ui.value = _ui.value.copy(isBusy = false, error = it.message ?: "Request failed") }
        }
    }

    private fun authHeader(): String? = _ui.value.token?.let { "Bearer $it" }
}

data class UiState(
    val token: String? = null,
    val currentUser: SessionUser? = null,
    val tasks: List<TaskItem> = emptyList(),
    val users: List<SessionUser> = emptyList(),
    val commissionPct: Int = 15,
    val finance: FinancePayload? = null,
    val stats: StatsPayload? = null,
    val error: String? = null,
    val message: String? = null,
    val isBusy: Boolean = false,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = SessionStore(this)
        setContent {
            MaterialTheme {
                val viewModel: MainViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(store) as T
                })
                SpacronApp(viewModel)
            }
        }
    }
}

@Composable
fun SpacronApp(viewModel: MainViewModel) {
    val nav = rememberNavController()
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val snackbars = remember { SnackbarHostState() }

    LaunchedEffect(state.error, state.message) {
        state.error?.let { snackbars.showSnackbar(it) }
        state.message?.let { snackbars.showSnackbar(it) }
    }

    NavHost(navController = nav, startDestination = if (state.currentUser == null) "auth" else "home") {
        composable("auth") {
            AuthScreen(
                onLogin = { email, password -> viewModel.login(email, password) },
                onRegister = { name, email, password, role, skills -> viewModel.register(name, email, password, role, skills) },
                onContinue = { if (viewModel.ui.value.currentUser != null) nav.navigate("home") { popUpTo("auth") { inclusive = true } } },
                signedIn = state.currentUser != null,
                isBusy = state.isBusy,
            )
        }
        composable("home") {
            HomeScreen(state = state, snackbars = snackbars, onLogout = {
                viewModel.logout()
                nav.navigate("auth") { popUpTo("home") { inclusive = true } }
            }, onRefresh = viewModel::refresh, onAcceptTask = viewModel::acceptTask, onSubmitTask = viewModel::submitTask, onCreateTask = viewModel::createTask, onPayTask = viewModel::payTask, onApproveTask = viewModel::approveTask, onRejectTask = viewModel::rejectTask, onUpdateCommission = viewModel::updateCommission, onUpdateUserStatus = viewModel::updateUserStatus)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SnackbarHost(hostState = snackbars, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun AuthScreen(
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String, String, List<String>) -> Unit,
    onContinue: () -> Unit,
    signedIn: Boolean,
    isBusy: Boolean,
) {
    var mode by rememberSaveable { mutableStateOf("login") }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val selectedSkills = remember { mutableStateListOf<String>() }
    val skills = listOf("Surveys", "Data Entry", "Writing", "QA Testing", "Design", "Transcription", "Moderation")

    LaunchedEffect(signedIn) {
        if (signedIn) onContinue()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF5F7F0), Color(0xFFE5F2EA), Color(0xFFF8F5EE)),
                ),
            )
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Spacron", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Marketplace migrated to Kotlin Android.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(selected = mode == "login", onClick = { mode = "login" }, label = { Text("Login") })
            FilterChip(selected = mode == "register", onClick = { mode = "register" }, label = { Text("Register") })
        }
        Spacer(Modifier.height(16.dp))
        if (mode == "register") {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
        }
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        if (mode == "register") {
            Spacer(Modifier.height(12.dp))
            var role by rememberSaveable { mutableStateOf("user") }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(selected = role == "user", onClick = { role = "user" }, label = { Text("User") })
                FilterChip(selected = role == "seller", onClick = { role = "seller" }, label = { Text("Seller") })
            }
            if (role == "user") {
                Spacer(Modifier.height(12.dp))
                Text("Skills", fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    skills.take(3).forEach { skill ->
                        FilterChip(
                            selected = skill in selectedSkills,
                            onClick = { if (skill in selectedSkills) selectedSkills.remove(skill) else selectedSkills.add(skill) },
                            label = { Text(skill) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                enabled = !isBusy,
                onClick = { onRegister(name, email, password, role, selectedSkills.toList()) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isBusy) "Creating..." else "Create account")
            }
        } else {
            Spacer(Modifier.height(16.dp))
            Button(
                enabled = !isBusy,
                onClick = { onLogin(email, password) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isBusy) "Signing in..." else "Sign in")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: UiState,
    snackbars: SnackbarHostState,
    onLogout: () -> Unit,
    onRefresh: () -> Unit,
    onAcceptTask: (Long) -> Unit,
    onSubmitTask: (Long, String, String) -> Unit,
    onCreateTask: (CreateTaskRequest) -> Unit,
    onPayTask: (Long) -> Unit,
    onApproveTask: (Long) -> Unit,
    onRejectTask: (Long) -> Unit,
    onUpdateCommission: (Int) -> Unit,
    onUpdateUserStatus: (Long, String) -> Unit,
) {
    val user = state.currentUser ?: return
    var submitText by rememberSaveable { mutableStateOf("") }
    var submitLink by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${user.name} • ${user.role}") },
                actions = {
                    OutlinedButton(onClick = onRefresh) { Text("Refresh") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = onLogout) { Text("Logout") }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbars) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                DashboardHeader(state)
            }
            when (user.role) {
                "user" -> {
                    val active = state.tasks.firstOrNull { it.assignedTo.equals(user.email, ignoreCase = true) && it.status == "ASSIGNED" }
                    if (active != null) {
                        item {
                            PanelCard("Active task") {
                                Text(active.title, fontWeight = FontWeight.SemiBold)
                                Text(active.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (active.requiresContact && active.contactInfo.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text("Contact: ${active.contactInfo}")
                                }
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(value = submitText, onValueChange = { submitText = it }, label = { Text("Work summary") }, modifier = Modifier.fillMaxWidth())
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(value = submitLink, onValueChange = { submitLink = it }, label = { Text("Link (optional)") }, modifier = Modifier.fillMaxWidth())
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { onSubmitTask(active.id, submitText, submitLink) }) { Text("Submit work") }
                            }
                        }
                    }
                    items(state.tasks.filter { it.status == "OPEN" }) { task ->
                        PanelCard(task.title) {
                            Text(task.description)
                            Spacer(Modifier.height(8.dp))
                            Text("$${task.price} • ${task.timeEstimateMinutes} min • ${task.difficulty}")
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { onAcceptTask(task.id) }) { Text("Accept task") }
                        }
                    }
                }
                "seller" -> {
                    item {
                        SellerCreateCard(user, state.commissionPct, onCreateTask)
                    }
                    items(state.tasks.filter { it.sellerId?.toString() == user.id }) { task ->
                        PanelCard(task.title) {
                            Text("${task.status} • $${task.price}")
                            Text(task.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (task.status == "PENDING_PAYMENT") {
                                    Button(onClick = { onPayTask(task.id) }) { Text("Pay & publish") }
                                }
                                if (task.status == "SUBMITTED") {
                                    Button(onClick = { onApproveTask(task.id) }) { Text("Approve") }
                                    OutlinedButton(onClick = { onRejectTask(task.id) }) { Text("Reject") }
                                }
                            }
                            task.submission?.let {
                                Spacer(Modifier.height(8.dp))
                                Text("Submission: ${it.text.ifBlank { it.link }}")
                            }
                        }
                    }
                }
                "admin" -> {
                    item {
                        AdminControls(state, onUpdateCommission)
                    }
                    items(state.users) { managedUser ->
                        PanelCard(managedUser.name) {
                            Text("${managedUser.email} • ${managedUser.role} • ${managedUser.status}")
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { managedUser.id.toLongOrNull()?.let { onUpdateUserStatus(it, "active") } }) { Text("Active") }
                                OutlinedButton(onClick = { managedUser.id.toLongOrNull()?.let { onUpdateUserStatus(it, "flagged") } }) { Text("Flag") }
                                OutlinedButton(onClick = { managedUser.id.toLongOrNull()?.let { onUpdateUserStatus(it, "suspended") } }) { Text("Suspend") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardHeader(state: UiState) {
    PanelCard("Overview") {
        Text("Users: ${state.stats?.userCount ?: 0}")
        Text("Tasks: ${state.stats?.taskCount ?: 0}")
        Text("Paid out: $${state.stats?.paidOutTotal ?: 0.0}")
        state.finance?.let {
            Spacer(Modifier.height(8.dp))
            Text("Platform wallet: $${it.platformWalletBalance}")
            Text("Commission total: $${it.commissionFromTasks}")
        }
    }
}

@Composable
fun SellerCreateCard(user: SessionUser, commissionPct: Int, onCreateTask: (CreateTaskRequest) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var reward by rememberSaveable { mutableStateOf("5") }
    var minutes by rememberSaveable { mutableStateOf("60") }
    var category by rememberSaveable { mutableStateOf("Survey") }
    var difficulty by rememberSaveable { mutableStateOf("Easy") }
    var contactInfo by rememberSaveable { mutableStateOf("") }

    PanelCard("Post a task") {
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = reward, onValueChange = { reward = it }, label = { Text("Reward") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = minutes, onValueChange = { minutes = it }, label = { Text("Minutes") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = difficulty, onValueChange = { difficulty = it }, label = { Text("Difficulty") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = contactInfo, onValueChange = { contactInfo = it }, label = { Text("Contact info") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            val sellerId = user.id.toLongOrNull() ?: return@Button
            onCreateTask(
                CreateTaskRequest(
                    title = title,
                    price = reward.toDoubleOrNull() ?: 0.0,
                    description = description,
                    timeEstimateMinutes = minutes.toIntOrNull() ?: 60,
                    difficulty = difficulty,
                    category = category,
                    postedBy = user.name,
                    sellerId = sellerId,
                    contactInfo = contactInfo,
                    requiresContact = contactInfo.isNotBlank(),
                    commissionRate = commissionPct / 100.0,
                ),
            )
        }) {
            Text("Create draft")
        }
    }
}

@Composable
fun AdminControls(state: UiState, onUpdateCommission: (Int) -> Unit) {
    var commission by rememberSaveable(state.commissionPct) { mutableStateOf(state.commissionPct.toFloat()) }
    PanelCard("Admin controls") {
        Text("Commission ${commission.toInt()}%")
        Slider(value = commission, onValueChange = { commission = it }, valueRange = 10f..20f)
        Button(onClick = { onUpdateCommission(commission.toInt()) }) { Text("Save commission") }
    }
}

@Composable
fun PanelCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.88f)),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            content()
        })
    }
}
