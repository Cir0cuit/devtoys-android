import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.CronType

fun main() {
    val defUnix = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)
    println(defUnix.fieldDefinitions.map { it.fieldName })
    val defSpring = CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING)
    println(defSpring.fieldDefinitions.map { it.fieldName })
}
