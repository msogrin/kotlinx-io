package kotlinx.io.compat

import kotlinx.io.*
import java.io.*


public val File.input: Input
    get() = inputStream().toInput()

public val File.output: Output
    get() = outputStream().toOutput()
