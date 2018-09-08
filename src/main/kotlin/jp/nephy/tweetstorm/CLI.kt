package jp.nephy.tweetstorm

import org.apache.commons.cli.*
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

fun parseCommandLine(args: Array<String>): CLIArguments {
    val parser = DefaultParser()
    val options = Options()
            .addOption(
                    Option.builder("help").desc("Print this help and exit.").build()
            )
            .addOption(
                    Option.builder("config").longOpt("config-path").hasArg().argName("config_path").numberOfArgs(1).desc("Specify Tweetstorm config.json path. (Default: ./config.json)").build()
            )

    val result = try {
        parser.parse(options, args)
    } catch (e: Exception) {
        when (e) {
            is UnrecognizedOptionException -> {
                System.err.println("Unknown option: ${e.option}")
            }
            is ParseException -> {
                System.err.println("Failed to parse command line.")
            }
        }

        printHelpAndExit(options)
        throw e
    }

    if (result.hasOption("help")) {
        printHelpAndExit(options)
    }

    val configPath = result.getOptionValue("config")?.split(File.separatorChar)?.let { Paths.get(it.first(), *it.drop(1).toTypedArray()) }
    return CLIArguments(configPath)
}

private fun printHelpAndExit(options: Options) {
    val header = "\nTweetstorm\n    A simple substitute implementation for the Twitter UserStream.\n\n============================================================\n\nAvailable cli options:"
    val footer = "\nContact:\n    Twitter: @SlashNephy\n    GitHub: https://github.com/SlashNephy/Tweetstorm\n\n    Feel free to report issues anytime."

    HelpFormatter().apply {
        leftPadding = 4
        width = 150
    }.printHelp("java -jar tweetstorm-full.jar", header, options, footer, true)

    System.exit(0)
}

data class CLIArguments(val configPath: Path?)
