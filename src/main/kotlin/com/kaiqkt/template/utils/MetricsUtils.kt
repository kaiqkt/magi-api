package ${package}.utils

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.stereotype.Component
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.util.concurrent.TimeUnit

@Component
class MetricsUtils(
    private val meterRegistry: MeterRegistry,
) {
    fun counter(
        name: String,
        vararg tags: String,
        value: Double = 1.0,
    ) {
        meterRegistry.counter(name, *tags).increment(value)
    }

    fun <T> timer(
        name: String,
        vararg tags: String,
        block: () -> T,
    ): T {
        val start = System.nanoTime()
        try {
            return block()
        } finally {
            val end = System.nanoTime()
            val durationSec = (end - start) / 1_000_000_000L

            Timer
                .builder(name)
                .publishPercentileHistogram(true)
                .publishPercentiles(0.5, 0.95, 0.99)
                .tags(*tags)
                .register(meterRegistry)
                .record(durationSec, TimeUnit.SECONDS)
        }
    }
}