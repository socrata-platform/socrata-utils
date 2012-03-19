package com.socrata.util.strings

import java.util.regex.Pattern

object CamelCase {
  final val CamelizeBreakPattern = Pattern.compile("[_\\./\\s]+")
  final val DecamelizeMatchPattern = Pattern.compile("(^[^A-Z]+|[A-Z][^A-Z]+)")

  def camelize(input: String): String = camelize(input, true)

  def camelize(input: String, firstIsUpper: Boolean): String = {
    if (input == null || input.length() == 0) // null for legacy reasons
      return input;

    val camelParts = for {
      part <- CamelizeBreakPattern.split(input.trim())
      if part.nonEmpty
    } yield part.substring(0, 1).toUpperCase + part.substring(1)

    val output = camelParts.mkString

    if (firstIsUpper) output
    else output.substring(0, 1).toLowerCase + output.substring(1)
  }

  def decamelize(input: String): String = {
    if (input == null || input.length() == 0) // null for legacy reasons
      return input;

    val matcher = DecamelizeMatchPattern.matcher(input);
    val output = new StringBuilder
    while (matcher.find()) {
      if (output.length > 0)
        output.append("_");
      output.append(matcher.group().toLowerCase);
    }
    output.toString();
  }
}
