package com.raquo.laminarexamples.components

import com.raquo.laminar.bundle._
import com.raquo.laminar.nodes.ReactiveNode

object Table {
  def apply(): ReactiveNode = {
    table(
      tr(td(colSpan := 2, "a"), td("b")),
      tr(td("1"), td("2"), td("3"))
    )
  }
}
