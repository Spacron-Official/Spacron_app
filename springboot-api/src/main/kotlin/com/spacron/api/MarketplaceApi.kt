package com.spacron.api

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.*
import kotlin.math.round

enum class UserRole { user, seller, admin }
enum class UserStatus { active, flagged, suspended }
enum class TaskStatus { PENDING_PAYMENT, OPEN, ASSIGNED, SUBMITTED, REJECTED, PAID }

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false)
    var email: String = "",
    @Column(unique = true, nullable = false)
    var normalizedEmail: String = "",
    @Column(nullable = false)
    var name: String = "",
    @Column(nullable = false)
    var password: String = "",
    @Enumerated(EnumType.STRING)
    var role: UserRole = UserRole.user,
    @ElementCollection(fetch = FetchType.EAGER)
    var skills: List<String> = emptyList(),
    var balance: Double = 0.0,
    var tasksCompleted: Int = 0,
    var rating: Double = 0.0,
    @Enumerated(EnumType.STRING)
    var status: UserStatus = UserStatus.active,
    var createdAt: Instant = Instant.now(),
)

@Entity
@Table(name = "tasks")
class TaskEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var title: String = "",
    var price: Double = 0.0,
    @Enumerated(EnumType.STRING)
    var status: TaskStatus = TaskStatus.PENDING_PAYMENT,
    var assignedTo: String? = null,
    @Column(length = 2000)
    var description: String = "",
    var timeEstimateMinutes: Int = 60,
    var difficulty: String = "Medium",
    var category: String = "General",
    var postedBy: String = "",
    var sellerId: Long? = null,
    var createdAt: Instant = Instant.now(),
    var commissionRate: Double = 0.15,
    var commission: Double = 0.0,
    var userEarning: Double = 0.0,
    var adminRevenue: Double = 0.0,
    var contactInfo: String = "",
    var requiresContact: Boolean = false,
    @Column(columnDefinition = "TEXT")
    var submissionJson: String? = null,
) {
    @get:Transient
    @set:Transient
    var submission: SubmissionDto?
        get() = submissionJson?.let { ObjectMapper().readValue(it, SubmissionDto::class.java) }
        set(value) { submissionJson = value?.let { ObjectMapper().writeValueAsString(it) } }
}

@Entity
@Table(name = "platform_config")
class PlatformConfigEntity(
    @Id
    var configKey: String = "",
    var configValue: String = "",
)

@Entity
@Table(name = "listing_payments")
class ListingPaymentEntity(
    @Id
    var id: String = UUID.randomUUID().toString(),
    var taskId: Long = 0,
    var amount: Double = 0.0,
    var sellerEmail: String = "",
    var type: String = "listing",
    var createdAt: Instant = Instant.now(),
)

@Entity
@Table(name = "sessions")
class SessionEntity(
    @Id
    var token: String = "",
    @Column(nullable = false)
    var userId: Long = 0,
    @Column(nullable = false)
    var expiresAt: Instant = Instant.now(),
)

@Repository
interface UserRepository : JpaRepository<UserEntity, Long> {
    fun findByNormalizedEmail(normalizedEmail: String): UserEntity?
}

@Repository
interface TaskRepository : JpaRepository<TaskEntity, Long>

@Repository
interface PlatformConfigRepository : JpaRepository<PlatformConfigEntity, String>

@Repository
interface ListingPaymentRepository : JpaRepository<ListingPaymentEntity, String>

@Repository
interface SessionRepository : JpaRepository<SessionEntity, String>

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(val success: Boolean, val data: T? = null, val error: String? = null)

data class SessionUserDto(
    val id: String,
    val email: String,
    val name: String,
    val role: UserRole,
    val skills: List<String>,
    val balance: Double,
    val tasksCompleted: Int,
    val rating: Double,
    val status: String,
    val joinedAt: String,
    val avatar: String,
)

data class AuthResponse(val token: String, val user: SessionUserDto)
data class StatsDto(val userCount: Int, val taskCount: Int, val completedCount: Int, val paidOutTotal: Double)
data class ConfigDto(val commissionPct: Int)
data class FinanceDto(
    val platformWalletBalance: Double,
    val listingPayments: List<ListingPaymentDto>,
    val commissionFromTasks: Double,
)

data class ListingPaymentDto(
    val id: String,
    val taskId: Long,
    val amount: Double,
    val sellerEmail: String,
    val type: String,
    val createdAt: String,
)

data class SubmissionDto(val text: String = "", val link: String = "", val file: String? = null)

data class TaskDto(
    val id: Long,
    val title: String,
    val price: Double,
    val status: String,
    val assignedTo: String?,
    val description: String,
    val timeEstimateMinutes: Int,
    val difficulty: String,
    val category: String,
    val postedBy: String,
    val sellerId: Long?,
    val createdAt: String,
    val commissionRate: Double,
    val commission: Double,
    val userEarning: Double,
    val adminRevenue: Double,
    val contactInfo: String,
    val requiresContact: Boolean,
    val submission: SubmissionDto?,
)

data class RegisterRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank val name: String,
    @field:NotBlank @field:Size(min = 3) val password: String,
    val role: UserRole,
    val skills: List<String>? = null,
)

data class LoginRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank val password: String,
)

data class CreateTaskRequest(
    @field:NotBlank val title: String,
    @field:Positive val price: Double,
    val description: String? = null,
    val timeEstimate: Int? = null,
    val timeEstimateMinutes: Int? = null,
    val difficulty: String? = null,
    val category: String? = null,
    val postedBy: String? = null,
    val sellerId: Long? = null,
    val contactInfo: String? = null,
    val requiresContact: Boolean? = null,
    val commissionRate: Double? = null,
)

data class AcceptTaskRequest(val assignedTo: String? = null, val email: String? = null)
data class SubmitTaskRequest(val text: String? = null, val link: String? = null, val fileName: String? = null, val fileBase64: String? = null)
data class UpdateStatusRequest(val status: String)
data class CommissionRequest(val commissionPct: Int)

class ApiException(val status: HttpStatus, override val message: String) : RuntimeException(message)

@Configuration
class SecurityConfig {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)
}

@Configuration
@EnableWebSocketMessageBroker
class SocketConfig : WebSocketMessageBrokerConfigurer {
    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker("/topic")
        config.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*")
    }
}

private fun normalizeEmail(email: String): String = email.trim().lowercase()

@Service
class SessionService(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val props: SpacronProperties,
) {
    fun createToken(userId: Long): String {
        val token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "")
        sessionRepository.save(
            SessionEntity(
                token = token,
                userId = userId,
                expiresAt = Instant.now().plusSeconds(props.sessionDays * 24 * 60 * 60),
            ),
        )
        return token
    }

    fun authenticatedUser(request: HttpServletRequest): UserEntity? {
        val header = request.getHeader("Authorization") ?: return null
        if (!header.startsWith("Bearer ")) return null
        val token = header.removePrefix("Bearer ").trim()
        val session = sessionRepository.findById(token).orElse(null) ?: return null
        if (session.expiresAt.isBefore(Instant.now())) {
            sessionRepository.deleteById(token)
            return null
        }
        return userRepository.findById(session.userId).orElse(null)
    }
}

@Service
@Validated
class MarketplaceService(
    private val users: UserRepository,
    private val tasks: TaskRepository,
    private val configs: PlatformConfigRepository,
    private val listingPayments: ListingPaymentRepository,
    private val sessions: SessionService,
    private val props: SpacronProperties,
    private val passwordEncoder: PasswordEncoder,
    private val broker: SimpMessagingTemplate,
) {
    private val uploadsDir: Path = Paths.get("uploads")

    init {
        Files.createDirectories(uploadsDir)
    }

    @EventListener(ApplicationReadyEvent::class)
    fun ensureAdmin() {
        try {
            val adminEmail = normalizeEmail("admin@example.com")
            val existing = users.findByNormalizedEmail(adminEmail)
            val fixedPassword = "change-me"
            
            if (existing == null) {
                users.save(
                    UserEntity(
                        email = "admin@example.com",
                        normalizedEmail = adminEmail,
                        name = "Admin",
                        password = passwordEncoder.encode(fixedPassword),
                        role = UserRole.admin,
                        status = UserStatus.active,
                    ),
                )
                println(">>> ADMIN CREATED: $adminEmail / $fixedPassword")
            } else {
                existing.role = UserRole.admin
                existing.status = UserStatus.active
                // ONLY re-encode if it's not already a BCrypt hash
                if (!existing.password.startsWith("$2a$")) {
                    existing.password = passwordEncoder.encode(fixedPassword)
                }
                users.save(existing)
                println(">>> ADMIN SYNCED: $adminEmail")
            }
        } catch (e: Exception) {
            println(">>> FAILED TO INITIALIZE ADMIN: ${e.message}")
            e.printStackTrace()
        }
    }

    fun hello() = mapOf("message" to "Spring Boot marketplace server is running.")

    fun requireUser(request: HttpServletRequest, vararg roles: UserRole): UserEntity {
        val user = sessions.authenticatedUser(request) ?: throw ApiException(HttpStatus.UNAUTHORIZED, "Authentication required.")
        if (user.status != UserStatus.active) {
            throw ApiException(HttpStatus.FORBIDDEN, "Account is not active.")
        }
        if (roles.isNotEmpty() && user.role !in roles) {
            throw ApiException(HttpStatus.FORBIDDEN, "You do not have access to this action.")
        }
        return user
    }

    fun optionalUser(request: HttpServletRequest): UserEntity? = sessions.authenticatedUser(request)

    fun register(input: RegisterRequest): AuthResponse {
        if (input.role == UserRole.admin) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Invalid role.")
        }
        val email = input.email.trim()
        if (users.findByNormalizedEmail(normalizeEmail(email)) != null) {
            throw ApiException(HttpStatus.CONFLICT, "An account with this email already exists.")
        }
        val user = users.save(
            UserEntity(
                email = email,
                normalizedEmail = normalizeEmail(email),
                name = input.name.trim(),
                password = passwordEncoder.encode(input.password),
                role = input.role,
                skills = input.skills ?: emptyList(),
                status = UserStatus.active,
            ),
        )
        val token = sessions.createToken(user.id!!)
        return AuthResponse(token = token, user = toSessionUser(user))
    }

    fun login(input: LoginRequest): AuthResponse {
        val normalized = normalizeEmail(input.email)
        println(">>> LOGIN ATTEMPT: $normalized")
        val user = users.findByNormalizedEmail(normalized)
            ?: throw ApiException(HttpStatus.UNAUTHORIZED, "User not found.")
        
        if (!passwordEncoder.matches(input.password, user.password)) {
            println(">>> PASSWORD MISMATCH: Received '${input.password}', Expected (Encrypted) '${user.password}'")
            throw ApiException(HttpStatus.UNAUTHORIZED, "Invalid password.")
        }
        if (user.status != UserStatus.active) {
            throw ApiException(HttpStatus.FORBIDDEN, "Account is not active.")
        }
        val token = sessions.createToken(user.id!!)
        println(">>> LOGIN SUCCESS: $normalized")
        return AuthResponse(token = token, user = toSessionUser(user))
    }

    fun stats(): StatsDto {
        val allUsers = users.findAll()
        val allTasks = tasks.findAll()
        val paid = allTasks.filter { it.status == TaskStatus.PAID }
        return StatsDto(
            userCount = allUsers.size,
            taskCount = allTasks.size,
            completedCount = paid.size,
            paidOutTotal = paid.sumOf { it.userEarning }.rounded(),
        )
    }

    fun getConfig(): ConfigDto = ConfigDto(currentCommissionPct())

    fun updateConfig(request: HttpServletRequest, payload: CommissionRequest): ConfigDto {
        requireUser(request, UserRole.admin)
        if (payload.commissionPct !in 10..20) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Commission must be between 10% and 20%")
        }
        configs.save(PlatformConfigEntity("PLATFORM_COMMISSION_PCT", (payload.commissionPct / 100.0).toString()))
        return getConfig()
    }

    fun allUsers(request: HttpServletRequest): List<SessionUserDto> {
        requireUser(request, UserRole.admin)
        return users.findAll().sortedBy { it.id }.map(::toSessionUser)
    }

    fun userById(request: HttpServletRequest, id: Long): SessionUserDto {
        val requester = requireUser(request)
        if (requester.role != UserRole.admin && requester.id != id) {
            throw ApiException(HttpStatus.FORBIDDEN, "Forbidden.")
        }
        return toSessionUser(users.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "User not found.") })
    }

    fun updateUserStatus(request: HttpServletRequest, id: Long, payload: UpdateStatusRequest): SessionUserDto {
        requireUser(request, UserRole.admin)
        val status = runCatching { UserStatus.valueOf(payload.status) }
            .getOrElse { throw ApiException(HttpStatus.BAD_REQUEST, "Valid status is required.") }
        val user = users.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "User not found.") }
        user.status = status
        return toSessionUser(users.save(user))
    }

    fun visibleTasks(request: HttpServletRequest): List<TaskDto> {
        val viewer = optionalUser(request)
        return tasks.findAll()
            .sortedByDescending { it.id }
            .filter { task ->
                when {
                    viewer == null -> task.status == TaskStatus.OPEN
                    viewer.role == UserRole.admin -> true
                    viewer.role == UserRole.seller -> task.status == TaskStatus.OPEN || task.sellerId == viewer.id
                    else -> task.status == TaskStatus.OPEN
                }
            }
            .map(::toTaskDto)
    }

    fun adminFinance(request: HttpServletRequest): FinanceDto {
        requireUser(request, UserRole.admin)
        val payments = listingPayments.findAll().sortedByDescending { it.createdAt }
        val commissionTotal = tasks.findAll().sumOf { it.adminRevenue }.rounded()
        return FinanceDto(
            platformWalletBalance = payments.sumOf { it.amount }.rounded(),
            listingPayments = payments.map {
                ListingPaymentDto(it.id, it.taskId, it.amount.rounded(), it.sellerEmail, it.type, it.createdAt.toString())
            },
            commissionFromTasks = commissionTotal,
        )
    }

    fun createTask(request: HttpServletRequest, payload: CreateTaskRequest): TaskDto {
        val seller = requireUser(request, UserRole.seller)
        val sellerId = payload.sellerId ?: seller.id!!
        if (sellerId != seller.id) {
            throw ApiException(HttpStatus.FORBIDDEN, "sellerId must match your account.")
        }
        val task = tasks.save(
            TaskEntity(
                title = payload.title.trim(),
                price = payload.price,
                status = TaskStatus.PENDING_PAYMENT,
                description = payload.description?.trim().orEmpty(),
                timeEstimateMinutes = maxOf(1, payload.timeEstimateMinutes ?: payload.timeEstimate ?: 60),
                difficulty = payload.difficulty ?: "Medium",
                category = payload.category ?: "General",
                postedBy = payload.postedBy ?: seller.name,
                sellerId = seller.id,
                commissionRate = clampCommissionRate(payload.commissionRate),
                contactInfo = payload.contactInfo?.trim().orEmpty(),
                requiresContact = payload.requiresContact ?: false,
            ),
        )
        val dto = toTaskDto(task)
        publishEvent("task:created", dto)
        return dto
    }

    fun payTask(request: HttpServletRequest, taskId: Long): TaskDto {
        val seller = requireUser(request, UserRole.seller)
        val task = tasks.findById(taskId).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "Task not found.") }
        if (task.sellerId != seller.id) {
            throw ApiException(HttpStatus.FORBIDDEN, "Forbidden.")
        }
        if (task.status != TaskStatus.PENDING_PAYMENT) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Payment is only allowed for tasks awaiting listing payment.")
        }
        task.status = TaskStatus.OPEN
        val updated = tasks.save(task)
        listingPayments.save(
            ListingPaymentEntity(
                taskId = updated.id!!,
                amount = updated.price,
                sellerEmail = seller.email,
            ),
        )
        val dto = toTaskDto(updated)
        publishEvent("task:published", dto)
        return dto
    }

    fun acceptTask(request: HttpServletRequest, taskId: Long, payload: AcceptTaskRequest): TaskDto {
        val user = requireUser(request, UserRole.user)
        val assignedEmail = (payload.assignedTo ?: payload.email ?: "").trim()
        if (assignedEmail.isBlank()) {
            throw ApiException(HttpStatus.BAD_REQUEST, "assignedTo (user email) is required.")
        }
        if (!assignedEmail.equals(user.email, ignoreCase = true)) {
            throw ApiException(HttpStatus.FORBIDDEN, "You can only accept tasks for your own account.")
        }
        val task = tasks.findById(taskId).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "Task not found.") }
        if (task.status != TaskStatus.OPEN) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Only tasks with status OPEN can be accepted.")
        }
        task.status = TaskStatus.ASSIGNED
        task.assignedTo = user.email
        val updated = tasks.save(task)
        val dto = toTaskDto(updated)
        publishEvent("task:accepted", dto)
        return dto
    }

    fun submitTask(request: HttpServletRequest, taskId: Long, payload: SubmitTaskRequest): TaskDto {
        val user = requireUser(request, UserRole.user)
        val task = tasks.findById(taskId).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "Task not found.") }
        if (task.status != TaskStatus.ASSIGNED) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Task must be assigned to you before submission.")
        }
        if (!task.assignedTo.equals(user.email, ignoreCase = true)) {
            throw ApiException(HttpStatus.FORBIDDEN, "Only the assigned user can submit work.")
        }
        val fileUrl = saveSubmissionFile(taskId, payload.fileBase64, payload.fileName)
        val text = payload.text?.trim().orEmpty()
        val link = payload.link?.trim().orEmpty()
        if (text.isBlank() && link.isBlank() && fileUrl == null) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Provide work: description text, a link, and/or a file.")
        }
        task.status = TaskStatus.SUBMITTED
        task.submission = SubmissionDto(text = text, link = link, file = fileUrl)
        val updated = tasks.save(task)
        val dto = toTaskDto(updated)
        publishEvent("task:submitted", dto)
        return dto
    }

    fun approveTask(request: HttpServletRequest, taskId: Long): TaskDto {
        val seller = requireUser(request, UserRole.seller)
        val task = tasks.findById(taskId).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "Task not found.") }
        if (task.sellerId != seller.id) {
            throw ApiException(HttpStatus.FORBIDDEN, "Forbidden.")
        }
        if (task.status != TaskStatus.SUBMITTED) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Only submitted tasks can be approved.")
        }
        val assigneeEmail = task.assignedTo?.trim().orEmpty()
        val assignee = users.findByNormalizedEmail(normalizeEmail(assigneeEmail))
            ?: throw ApiException(HttpStatus.BAD_REQUEST, "Assignee missing for this task.")
        val rate = clampCommissionRate(task.commissionRate)
        val commission = (task.price * rate).rounded()
        val userEarning = (task.price - commission).rounded()
        assignee.balance += userEarning
        assignee.tasksCompleted += 1
        users.save(assignee)
        users.findByNormalizedEmail(normalizeEmail(props.adminEmail))?.let {
            it.balance += commission
            users.save(it)
        }
        task.status = TaskStatus.PAID
        task.commissionRate = rate
        task.commission = commission
        task.userEarning = userEarning
        task.adminRevenue = commission
        val updated = tasks.save(task)
        val dto = toTaskDto(updated)
        publishEvent("task:approved", dto)
        return dto
    }

    fun rejectTask(request: HttpServletRequest, taskId: Long): TaskDto {
        val seller = requireUser(request, UserRole.seller)
        val task = tasks.findById(taskId).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "Task not found.") }
        if (task.sellerId != seller.id) {
            throw ApiException(HttpStatus.FORBIDDEN, "Forbidden.")
        }
        if (task.status != TaskStatus.SUBMITTED) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Only submitted tasks can be rejected.")
        }
        task.status = TaskStatus.REJECTED
        val updated = tasks.save(task)
        val dto = toTaskDto(updated)
        publishEvent("task:rejected", dto)
        return dto
    }

    private fun currentCommissionPct(): Int {
        val raw = configs.findById("PLATFORM_COMMISSION_PCT").orElse(null)?.configValue
        val value = raw?.toDoubleOrNull() ?: props.defaultCommissionPct / 100.0
        return (clampCommissionRate(value) * 100).toInt()
    }

    private fun clampCommissionRate(rate: Double?): Double {
        val fallback = props.defaultCommissionPct / 100.0
        val number = rate ?: fallback
        return number.coerceIn(0.10, 0.20)
    }

    private fun toSessionUser(user: UserEntity): SessionUserDto {
        val parts = user.name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val avatar = when {
            parts.size >= 2 -> "${parts.first().first()}${parts.last().first()}".uppercase()
            parts.isNotEmpty() -> parts.first().take(2).uppercase()
            else -> "?"
        }
        return SessionUserDto(
            id = (user.id ?: 0L).toString(),
            email = user.email,
            name = user.name,
            role = user.role,
            skills = user.skills,
            balance = user.balance.rounded(),
            tasksCompleted = user.tasksCompleted,
            rating = user.rating,
            status = user.status.name,
            joinedAt = user.createdAt.toString(),
            avatar = avatar,
        )
    }

    private fun toTaskDto(task: TaskEntity): TaskDto {
        return TaskDto(
            id = task.id ?: 0L,
            title = task.title,
            price = task.price.rounded(),
            status = task.status.name,
            assignedTo = task.assignedTo,
            description = task.description,
            timeEstimateMinutes = task.timeEstimateMinutes,
            difficulty = task.difficulty,
            category = task.category,
            postedBy = task.postedBy,
            sellerId = task.sellerId,
            createdAt = task.createdAt.toString(),
            commissionRate = task.commissionRate,
            commission = task.commission.rounded(),
            userEarning = task.userEarning.rounded(),
            adminRevenue = task.adminRevenue.rounded(),
            contactInfo = task.contactInfo,
            requiresContact = task.requiresContact,
            submission = task.submission,
        )
    }

    private fun saveSubmissionFile(taskId: Long, fileBase64: String?, fileName: String?): String? {
        if (fileBase64.isNullOrBlank() || fileName.isNullOrBlank()) return null
        val bytes = try {
            Base64.getDecoder().decode(fileBase64)
        } catch (_: IllegalArgumentException) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Invalid file.")
        }
        if (bytes.size > 2 * 1024 * 1024) {
            throw ApiException(HttpStatus.BAD_REQUEST, "File too large (max 2MB).")
        }
        val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val storedName = "task-$taskId-${System.currentTimeMillis()}-$safeName"
        val target = uploadsDir.resolve(storedName)
        Files.write(target, bytes)
        return "/uploads/$storedName"
    }

    private fun publishEvent(name: String, payload: TaskDto) {
        broker.convertAndSend("/topic/tasks", mapOf("event" to name, "task" to payload))
    }
}

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(ApiException::class)
    fun handleApiException(error: ApiException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity.status(error.status).body(ApiResponse(success = false, error = error.message))
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException::class)
    fun handleValidationException(error: org.springframework.web.bind.MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val msg = error.bindingResult.fieldErrors.joinToString { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse(success = false, error = msg))
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(error: Exception): ResponseEntity<ApiResponse<Nothing>> {
        error.printStackTrace()
        val message = when {
            error.message?.contains("Connection refused") == true -> "Database connection failed. Please check if PostgreSQL is running."
            error.message?.contains("column \"id\"") == true -> "Database structure error. Please reset your tables."
            else -> error.message ?: "An unexpected internal error occurred."
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse(success = false, error = message))
    }
}

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping
class MarketplaceController(private val service: MarketplaceService) {
    @GetMapping("/")
    fun root(): Map<String, String> = service.hello()

    @GetMapping("/api/stats")
    fun stats() = ok(service.stats())

    @GetMapping("/uploads/{fileName:.+}")
    fun uploadedFile(@PathVariable fileName: String): ResponseEntity<Resource> {
        val path = Paths.get("uploads").resolve(fileName).normalize()
        val resource = UrlResource(path.toUri())
        return if (resource.exists()) ResponseEntity.ok(resource) else ResponseEntity.notFound().build()
    }

    @GetMapping("/api/config")
    fun config() = ok(service.getConfig())

    @PostMapping("/api/config")
    fun updateConfig(request: HttpServletRequest, @Valid @RequestBody payload: CommissionRequest) = ok(service.updateConfig(request, payload))

    @PostMapping("/api/register")
    fun register(@Valid @RequestBody payload: RegisterRequest) = ok(service.register(payload), HttpStatus.CREATED)

    @PostMapping("/api/login")
    fun login(@Valid @RequestBody payload: LoginRequest) = ok(service.login(payload))

    @GetMapping("/api/users")
    fun allUsers(request: HttpServletRequest) = ok(service.allUsers(request))

    @GetMapping("/api/users/{id}")
    fun userById(request: HttpServletRequest, @PathVariable id: Long) = ok(service.userById(request, id))

    @PatchMapping("/api/users/{id}")
    fun updateStatus(request: HttpServletRequest, @PathVariable id: Long, @Valid @RequestBody payload: UpdateStatusRequest) =
        ok(service.updateUserStatus(request, id, payload))

    @GetMapping("/api/tasks")
    fun tasks(request: HttpServletRequest) = ok(service.visibleTasks(request))

    @GetMapping("/api/admin/finance")
    fun adminFinance(request: HttpServletRequest) = ok(service.adminFinance(request))

    @PostMapping("/api/tasks")
    fun createTask(request: HttpServletRequest, @Valid @RequestBody payload: CreateTaskRequest) = ok(service.createTask(request, payload), HttpStatus.CREATED)

    @PostMapping("/api/tasks/{id}/pay")
    fun payTask(request: HttpServletRequest, @PathVariable id: Long) = ok(service.payTask(request, id))

    @PostMapping("/api/tasks/{id}/accept")
    fun acceptTask(request: HttpServletRequest, @PathVariable id: Long, @Valid @RequestBody payload: AcceptTaskRequest) =
        ok(service.acceptTask(request, id, payload))

    @PostMapping("/api/tasks/{id}/submit")
    fun submitTask(request: HttpServletRequest, @PathVariable id: Long, @Valid @RequestBody payload: SubmitTaskRequest) =
        ok(service.submitTask(request, id, payload))

    @PostMapping("/api/tasks/{id}/approve")
    fun approveTask(request: HttpServletRequest, @PathVariable id: Long) = ok(service.approveTask(request, id))

    @PostMapping("/api/tasks/{id}/reject")
    fun rejectTask(request: HttpServletRequest, @PathVariable id: Long) = ok(service.rejectTask(request, id))

    private fun <T> ok(data: T, status: HttpStatus = HttpStatus.OK): ResponseEntity<ApiResponse<T>> =
        ResponseEntity.status(status).body(ApiResponse(success = true, data = data))
}

private fun Double.rounded(): Double = round(this * 100.0) / 100.0
