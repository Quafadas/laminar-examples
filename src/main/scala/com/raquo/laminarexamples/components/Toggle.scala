package com.raquo.laminarexamples.components

import com.raquo.laminar.bundle._
import com.raquo.laminar.emitter.EventBus
import com.raquo.laminar.nodes.ReactiveElement
import com.raquo.xstream.XStream
import org.scalajs.dom
import org.scalajs.dom.raw.{HTMLInputElement, MouseEvent}

import scala.util.Random

class Toggle private (
  val $checked: XStream[Boolean],
  val node: ReactiveElement[dom.html.Div]
)

object Toggle {
  // @TODO how do we make this a controlled component?
  def apply(caption: String = "TOGGLE MEEEE"): Toggle = {
    val clickBus = new EventBus[MouseEvent]
    val $checked = clickBus.$.map(ev => ev.target.asInstanceOf[HTMLInputElement].checked)//.debug("$checked")

    // This will only be evaluated once
    val rand = Random.nextInt(99)

    val checkbox = input.apply(
      id := "toggle" + rand,
      className := "red",
      `type` := "checkbox",
      onClick --> clickBus
    )

    val $captionNode = $checked.map(checked => span(if (checked) "ON" else "off"))

    val node = div(
      className := "Toggle",
      checkbox,
      label(forId := "toggle" + rand, caption),
      " — ",
      child <-- $captionNode
    )

    new Toggle($checked, node)
  }
}
