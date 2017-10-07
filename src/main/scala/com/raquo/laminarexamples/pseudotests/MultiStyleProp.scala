package com.raquo.laminarexamples.pseudotests

import com.raquo.laminar.implicits._
import com.raquo.laminar.nodes.ReactiveNode
import com.raquo.laminar.props._
import com.raquo.laminar.styles._
import com.raquo.laminar.tags._
import com.raquo.laminarexamples.components.Toggle

object MultiStyleProp {

  def apply(): ReactiveNode = {

    val toggle = Toggle("Big")
    val toggle2 = Toggle("Red")

    val $fontSize = toggle.$checked.startWith(true).map(checked => if (checked) "45px" else "30px")
    val $fontColor = toggle2.$checked.startWith(true).map(checked => if (checked) "red" else "lime")

    div(
      className := "yolo",
      h1("MultiStyleProp"),
      toggle.node,
      toggle2.node,
      div(
        color <-- $fontColor,
        fontSize <-- $fontSize,
        span("HELLO")
      )
    )
  }
}
