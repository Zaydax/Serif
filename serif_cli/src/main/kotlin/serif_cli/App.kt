/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package xyz.room409.serif.serif_cli
import xyz.room409.serif.serif_shared.*

import java.io.Console

val console = System.console()

sealed class AppState {
    abstract fun transition(): AppState?
}
class Login(val mclient: MatrixClient): AppState() {
    override fun transition(): AppState? {
        print("Username: ")
        val username = console.readLine()
        print("Password: ")
        val password = String(console.readPassword())
        println("Logging in with username |$username| and a password I won't print...")
        return Rooms(mclient.login(username, password))
    }
}
class Rooms(val msession: MatrixSession): AppState() {
    override fun transition(): AppState? {
        println("Logged in! Sending test message")
        println(msession.test())
        return null
    }
}

class App {
    val mclient = MatrixClient()
    val version: String
        get() {
            return mclient.version() + ", CLI UI"
        }
    fun run() {
        var state: AppState? = Login(mclient)
        while (state != null) {
            state = state.transition()
        }
    }
}

fun main(args: Array<String>) {
    var app = App()
    println("Welcome to " + app.version)
    if (console == null) {
        println()
        println("Console was null! Probs running under gradle.")
        println("It doesn't work, and is really irritating")
        println("Try ./run_dist.sh if you're on a Unixy OS")
        println("Otherwise, if you're on Windows, you'll have to manually")
        println("do ./gradlew :serif_cli:assembleDist and then find and")
        println("extract the distribution zip, and then run the batch file")
        println("in the bin folder. Probs best to automate the process")
        println("like run_dist.sh, but I'm not on Windows and can't")
        println("a batch script without a reference. My apologies.")
        println("-Nathan")
        println()
    } else {
        app.run()
        println("Exiting!")
    }
}