import kotlinx.io.*
import kotlinx.io.Console
import kotlinx.io.bytes.*
import kotlinx.io.compat.*
import kotlinx.io.text.*
import java.io.*


class CSV(
    private val header: List<String>,
    private val columns: List<MutableList<String>>
) {
    private val height: Int = columns.firstOrNull()?.size ?: 0

    init {
        check(columns.all { it.size == height })
    }

    operator fun get(name: String): MutableList<String> = columns[header.indexOf(name)]

    operator fun get(index: Int): List<String> = columns.map { it[index] }

    fun writeTo(output: Output) {
        header.forEach {
            output.writeUtf8String(it)
            output.writeUtf8Char(',')
        }

        output.writeUtf8Char('\n')

        for (row in 0 until height) {
            columns.forEachIndexed { index, _ ->
                output.writeUtf8String(columns[index][row])
                if (index + 1 != columns.size) {
                    output.writeUtf8Char(',')
                }
            }

            output.writeUtf8Char('\n')
        }
    }
}


fun main(args: Array<String>) {
    val input = if (args.count() == 2) {
        File(args[1]).input
    } else {
        buildInput {
            writeUtf8String("x,2 * x,3 * x\n")
            repeat(100) {
                writeUtf8String("$it,${it * 2},${it * 3}\n")
            }
        }
    }

    val header = mutableListOf<String>()
    header += input.readUtf8Line().split(",")

    val columns = List<MutableList<String>>(header.size) { mutableListOf() }

    var column = 0
    while (!input.eof()) {
        columns[column] += input.readUtf8StringUntilDelimiters(",\n")
        column = (column + 1) % header.size
    }

    val result = CSV(header, columns)

    result.writeTo(Console.output)
}