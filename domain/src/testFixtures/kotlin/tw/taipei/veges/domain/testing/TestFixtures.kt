package tw.taipei.veges.domain.testing

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import tw.taipei.veges.domain.RefreshRepository
import tw.taipei.veges.domain.RefreshState

/** A clock with an explicit instant, suitable for date-sensitive domain tests. */
class FixedClock(initialInstant: Instant) : Clock() {
    private var currentInstant = initialInstant

    override fun getZone() = ZoneOffset.UTC

    override fun withZone(zone: java.time.ZoneId): Clock = this

    override fun instant(): Instant = currentInstant

    fun advanceByMillis(millis: Long) {
        currentInstant = currentInstant.plusMillis(millis)
    }
}

/** All asynchronous test work uses one controllable dispatcher. */
class TestDispatchers(
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) {
    val io: CoroutineDispatcher = dispatcher
    val default: CoroutineDispatcher = dispatcher
    val main: CoroutineDispatcher = dispatcher

    fun scope(): TestScope = TestScope(dispatcher)
}

/** A deterministic repository fake for offline, failure, and refresh-state tests. */
class FakeRefreshRepository(
    initialState: RefreshState = RefreshState.Idle,
    private val refreshResult: RefreshState = RefreshState.Idle,
) : RefreshRepository {
    private val mutableState = MutableStateFlow(initialState)
    private val refreshCountValue = AtomicInteger(0)

    override val state: Flow<RefreshState> = mutableState.asStateFlow()

    val refreshCount: Int
        get() = refreshCountValue.get()

    override suspend fun refresh() {
        refreshCountValue.incrementAndGet()
        mutableState.value = RefreshState.Refreshing
        mutableState.value = refreshResult
    }

    fun emit(state: RefreshState) {
        mutableState.value = state
    }
}

object FixtureLoader {
    fun read(name: String): String =
        requireNotNull(javaClass.classLoader?.getResourceAsStream("fixtures/$name")) {
            "Missing test fixture: fixtures/$name"
        }.bufferedReader().use { it.readText() }
}
