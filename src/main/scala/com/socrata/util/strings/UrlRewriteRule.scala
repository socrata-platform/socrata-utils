package com.socrata.util.strings

import java.util.regex.{Matcher, Pattern}
import org.slf4j.{Logger,LoggerFactory}
import collection.mutable.MutableList

@deprecated("Do not use this class.")
class UrlRewriteRule {

  private var log: Logger = LoggerFactory.getLogger(classOf[UrlRewriteRule])

  private case class Rule(pattern: Pattern, template: String) {}

  def add(pattern: String, template: String): UrlRewriteRule = {
    rules += Rule(Pattern.compile(pattern), template)
    return this
  }

  def rewrite(path: String): String = {
    if (path == null) return path

    for (r <- rules) {

      val matcher : Matcher = r.pattern.matcher(path)

      if (matcher.matches) {
        var rewritten: String = r.template
        var i: Int = 1

        while (i <= matcher.groupCount) {
          rewritten = rewritten.replaceFirst("\\$" + Integer.toString(i), Matcher.quoteReplacement(matcher.group(i)))
          i += 1
        }

        log.info("url rewrite: " + path + " : " + rewritten)
        return rewritten
      }
    }

    return path
  }

  private val rules = new MutableList[Rule]()
}
