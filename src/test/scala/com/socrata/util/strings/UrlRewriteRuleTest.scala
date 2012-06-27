package com.socrata.util.strings

import org.scalatest.prop.Checkers
import org.scalatest.FunSuite

class UrlRewriteRuleTest  extends FunSuite with Checkers {

  def semanticUrls {
    val rules = new UrlRewriteRule
    rules.add("^/id/(.*)/(.*)$", "/resources/$1/rows/$2").add("^/id/([^\\.]*)([^/]*)$", "/resources/$1/rows$2")
    assert("/resources/catalog/rows" == rules.rewrite("/id/catalog"))
    assert("/resources/catalog/rows.json" == rules.rewrite("/id/catalog.json"))
    assert("/resources/catalog/rows/abcd-1234" == rules.rewrite("/id/catalog/abcd-1234"))
    assert("/resources/catalog/rows/abcd-1234.json" == rules.rewrite("/id/catalog/abcd-1234.json"))
    assert("/others" == rules.rewrite("/others"))
    assert("/resources/catalog/rows.json$" == rules.rewrite("/id/catalog.json$"))
    assert("/resources/catalog/rows.json?col=value" == rules.rewrite("/id/catalog.json?col=value"))
  }

  test("UrlRewrite semanticUrls") { semanticUrls }
}