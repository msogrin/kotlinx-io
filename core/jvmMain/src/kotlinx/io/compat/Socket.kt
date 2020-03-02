package kotlinx.io.compat

import kotlinx.io.*
import java.net.*

public val Socket.input: Input
    get() = getInputStream().toInput()

public val Socket.output: Output
    get() = getOutputStream().toOutput()
