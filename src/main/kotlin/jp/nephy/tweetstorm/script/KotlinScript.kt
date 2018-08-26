package jp.nephy.tweetstorm.script

import jp.nephy.tweetstorm.logger
import org.jetbrains.kotlin.utils.addToStdlib.measureTimeMillisWithResult
import java.nio.file.Path
import javax.script.ScriptEngineManager
import kotlin.system.measureTimeMillis

class KotlinScript(private val path: Path) {
    val name = path.toString()

    private val logger = logger("Tweetstorm.KotlinScript [$name]")
    private val engine = ScriptEngineManager().getEngineByName("kotlin")

    private val content: String
        get() = path.toFile().reader().use {
            it.readText()
        }

    fun load() {
        val compileTime = measureTimeMillis {
            engine.eval(content)
        }
        logger.debug { "Load: $name finished in $compileTime ms." }
    }

    fun instance(className: String): Any {
        return engine.eval("$className()")
    }

    fun eval(source: String): Any? {
        val (evalTime, result: Any?) = measureTimeMillisWithResult {
            engine.eval(source)
        }
        logger.debug { "Eval: $name finished in $evalTime ms." }

        return result
    }
}
