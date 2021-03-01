package webcomponents

import com.raquo.laminar.api.L._
import org.scalajs.dom
import webcomponents.material._

import scala.scalajs.js
import webcomponents.vega._
import scala.scalajs.js.JSON
import scala.concurrent.Promise

object WebComponentsPage {

  // Better hope this matches the CSS...
  val vizDivClass = "viz"
  val randomGen = scala.util.Random

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  def apply(): Div = {
    
    val actionVar = Var("Do the thing")
    val iconVar = Var("<>")
    val progressVar = Var(0.5d)
    val config = JSON.parse("""{"logLevel": 0}""")
    val asObj = JSON.parse(spec)
    
    // This signal comes from a JS Promise... that's nice for third party integration.
    val stream = EventStream.fromJsPromise(VegaEmbed.embed(s"#$vizDivClass", asObj,config)).toWeakSignal
    val manageViewObj = stream.map( _.map(_.view.asInstanceOf[VegaView]))
    val randomVar = progressVar.signal.map( _ => randomGen.nextDouble()+0.5)

    // This stitches together the three things we would "need" to update the bar. The "data, "view", and a random number...
    val updateVizStream = progressVar.signal.combineWith(manageViewObj).combineWith(randomVar)

    div(
      h1("Web Components"),
      p(
        label("Button label: "),
        input(
          value <-- actionVar.signal,
          inContext { thisNode => onInput.mapTo(thisNode.ref.value) --> actionVar.writer}
        )
      ),
      p(
        label("Button icon: "),
        input(
          value <-- iconVar.signal,
          inContext { thisNode => onInput.mapTo(thisNode.ref.value) --> iconVar.writer}
        )
      ),
      p(
        "The button below is a ",
        a(
          href := "https://github.com/material-components/material-components-web-components/tree/master/packages/button",
          "@material/mwc-button"
        ),
        " web component"
      ),
      p(
        Button(
          _.id := "myButton",
          _.label <-- actionVar.signal,
          //_.icon := "code",
          _.styles.mdcThemePrimary := "#ff0000",
          _ => onClick --> (_ => dom.window.alert("Click")), // standard event
          _.onMouseOver --> (_ => println("MouseOver")), // "custom" event
          _.slots.icon(span(child.text <-- iconVar.signal)),
          //_ => onMountCallback(ctx => ctx.thisNode.ref.doThing()) // doThing is not implemented, just for reference
        )
      ),
      p(
        p(
          "The progress bar below is a ",
          a(
            href := "https://github.com/material-components/material-components-web-components/tree/master/packages/linear-progress",
            "@material/mwc-linear-progress"
          ),
          " web component"
        ),
        div(
          LinearProgressBar(
            _.progress <-- progressVar.signal,
          )
        ),
        p(
          "The slider below is a ",
          a(
            href := "https://github.com/material-components/material-components-web-components/tree/master/packages/slider",
            "@material/mwc-slider"
          ),
          " web component"
        ),
        div(
          Slider(
            _.pin := true,
            _.min := 0,
            _.max := 1,
            _.value := progressVar.now(),
             _ => onMountCallback(ctx => {
               js.timers.setTimeout(1) {
                 // This component initializes its mdcFoundation asynchronously,
                 // so we need a short delay before accessing .layout() on it.
                 // To clarify, thisNode.ref already exists on mount, but the web component's
                 // implementation of layout() depends on thisNode.ref.mdcFoundation, which is
                 // populated asynchronously for some reason so it's not available on mount.
                 dom.console.log(ctx.thisNode.ref.layout())
               }
             }),
            slider => inContext { thisNode => {
                slider.onInput.mapTo(thisNode.ref.value) --> progressVar                
              } 
            }
          )          
        ), 
        p("The below bar chart is linked up to the slider... return the value on the right, and a random stream on the left"),           
        div(              
          cls := vizDivClass,
          updateVizStream.signal --> ( {case(value, view, random) => {                      
                  val data = JSON.parse(s"""[
                    {"category": "A", "amount": ${value*100}}, 
                    {"category": "B", "amount": ${value*50*random}}
                  ]""")
                  view match {
                    case Some(view) => 
                      view.data("table", data)                                     
                      view.runAsync() 
                    case _ => ()
                  }       
                }
              }
            ),          
          idAttr := vizDivClass
        )                       
      )
    )
  }


  val spec : String = """{
  "$schema": "https://vega.github.io/schema/vega/v5.json",
  "description": "A basic bar chart example, with value labels shown upon mouse hover.",
  "signals" : [
    {
          "name": "width",
          "init": "isFinite(containerSize()[0]) ? containerSize()[0] : 200",
          "on": [
            {
              "update": "isFinite(containerSize()[0]) ? containerSize()[0] : 200",
              "events": "window:resize"
            }
          ]
        },
        {
          "name": "height",
          "init": "isFinite(containerSize()[1]) ? containerSize()[1] : 200",
          "on": [
            {
              "update": "isFinite(containerSize()[1]) ? containerSize()[1] : 200",
              "events": "window:resize"
            }
          ]
        }, {
          "name": "tooltip",
          "value": {},
          "on": [
            {"events": "rect:mouseover", "update": "datum"},
            {"events": "rect:mouseout",  "update": "{}"}
          ]
        }
    ],

  "data": [
    {
      "name": "table",
      "values": [
        {"category": "A", "amount": 28},
        {"category": "B", "amount": 55}
      ]
    }
  ],

  "scales": [
    {
      "name": "xscale",
      "type": "band",
      "domain": {"data": "table", "field": "category"},    
      "range": "width",
      "padding": 0.05,
      "round": true
    },
    {
      "name": "yscale",
      "domain": {"data": "table", "field": "amount"},
      "domainMax": 100,
      "nice": true,
      "range": "height"
    }
  ],

  "axes": [
    { "orient": "bottom", "scale": "xscale" },
    { "orient": "left", "scale": "yscale" }
  ],

  "marks": [
    {
      "type": "rect",
      "from": {"data":"table"},
      "encode": {
        "enter": {
          "x": {"scale": "xscale", "field": "category"},
          "width": {"scale": "xscale", "band": 1},
          "y": {"scale": "yscale", "field": "amount"},
          "y2": {"scale": "yscale", "value": 0}
        },
        "update": {
          "fill": {"value": "steelblue"}
        },
        "hover": {
          "fill": {"value": "red"}
        }
      }
    },
    {
      "type": "text",
      "encode": {
        "enter": {
          "align": {"value": "center"},
          "baseline": {"value": "bottom"},
          "fill": {"value": "#333"}
        },
        "update": {
          "x": {"scale": "xscale", "signal": "tooltip.category", "band": 0.5},
          "y": {"scale": "yscale", "signal": "tooltip.amount", "offset": -2},
          "text": {"signal": "tooltip.amount"},
          "fillOpacity": [
            {"test": "datum === tooltip", "value": 0},
            {"value": 1}
          ]
        }
      }
    }
  ]
}
"""  
}

