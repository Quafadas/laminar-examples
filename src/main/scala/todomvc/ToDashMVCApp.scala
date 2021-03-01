package todomvc

import com.raquo.laminar.api.L._
import org.scalajs.dom.ext.KeyCode
import org.scalajs.dom
import scala.scalajs.js.Dynamic
import scala.scalajs.js.JSON
import webcomponents.vega.VegaEmbed
import webcomponents.vega.VegaView
import java.awt.Event
import scala.scalajs.js.timers._
import com.raquo.airstream.timing.PeriodicEventStream


// Piggy backing on top of the To Do app, and the web components, we can have a fun "dashboard"
object ToDashMvcApp extends ToDashMvcApp

trait ToDashMvcApp extends TodoMvcApp {
  val r = scala.util.Random  
  val seed = List("Laminar", "More", "More", "How do you like it how do you like it", "more", "laminar", "do more laminar", "get better at laminar").zipWithIndex.map({case(s,i) => TodoItem(i,s, r.nextDouble > 0.5 )})
  itemsVar.set(seed)
  val wordObserver = Observer[String](onNext = ev => dom.console.log(ev))  
  // Will need to match the CSS... 
  val vizDivCloudClass = "vizCloud"
  val vizDivPieClass = "vizPie"
  val eventBusWord : EventBus[String] = new EventBus[String]
  val (stream1, callback) = EventStream.withCallback[String]
  
  val activeWord: Signal[Option[String]] = stream1.toWeakSignal.map{ s => 
    dom.console.log(s)
    s match {
      case None => None
      case Some(value) => if(value == "NaN") {None} else {Some(value)}
    }
  }
  private var highlightLatest = Var[Boolean](true)

  
  def alarmStyles($isActive: Signal[Boolean]): Mod[HtmlElement] = Seq(
        styleAttr("transition: 0.8s;"),
        background <-- $isActive.flatMap {
          if (_) EventStream.periodic(1000).toSignal(0).map { i =>
            if (i % 2 == 0) "red"
            else "white"
          }
          else Val("white")
        }
      )

  override def render() : HtmlElement = {
    val parentDiv = super.render()    
    val config = JSON.parse("""{"logLevel": 0}""")    

    // This signal comes from a JS Promise... that's nice for third party integration.    
    val cloudStream: Signal[Option[Dynamic]] = EventStream.fromJsPromise(VegaEmbed.embed(s"#$vizDivCloudClass", JSON.parse(wordCloudSpec), config)).toWeakSignal
    val pieStream: Signal[Option[Dynamic]] = EventStream.fromJsPromise(VegaEmbed.embed(s"#$vizDivPieClass", JSON.parse(pieSpec), config)).toWeakSignal
    val manageViewObj = cloudStream.map( _.map(_.view.asInstanceOf[VegaView]))   
    val managePieViewObj = pieStream.map( _.map(_.view.asInstanceOf[VegaView]))
    

    

    val updateWordCloudVizStream = manageViewObj.combineWith(itemsVar.signal)
    val updatePieVizStream = managePieViewObj.combineWith(itemsVar.signal)

    div(
      cls:="viz",
      idAttr := "viz",
      parentDiv,
      label(
        child.text <-- stream1 
      ),
      div( 
        idAttr := vizDivCloudClass, 
        cls := vizDivCloudClass,
        updateWordCloudVizStream.signal --> ( 
          {
            case(view, value) => {                                              
              val words = value.filter(!_.completed).map(_.text).mkString(",")
              
              val active: scala.scalajs.js.Function2[String, Dynamic, Dynamic] = {
                (s, value) => {        
                  //dom.console.log(s, value)
                  val str = value.toString()
                  //wordObserver.contracollect(str)
                  if (!str.isEmpty()){
                    callback(str)
                  }
                  value
                }
              }

              val data = JSON.parse(s"""["$words"]""")
              
              view match {
                case Some(view) => 
                  view.data("table", data)
                  view.addSignalListener("active", active )
                  view.runAsync() 
                case _ => ()
              }
            }
          }
        )        
      ) ,
       div( 
        idAttr := vizDivPieClass, 
        cls := vizDivPieClass,
        updatePieVizStream.signal --> ( 
          {
            case(view, value) => {                                              
              val words = value.groupBy(_.completed)
              dom.console.log(words)

              val arrayData = scala.scalajs.js.Array[scala.scalajs.js.Object]()
              words.toVector.map{
                case(completed, items) => {
                  val temp = (completed, items) match {
                    case (true, items) => Dynamic.literal(field = items.length, id ="completed")
                    case (false, items) => Dynamic.literal(field = items.length, id = "open")
                  }
                  arrayData.push(temp)
                }
              }

              dom.console.log(arrayData)
              view match {
                case Some(view) => 
                  view.data("table", arrayData)
                  view.runAsync() 
                case _ => ()
              }       
            }
          }
        ) 
       )
    ) 
  }

    // Render a single item. Note that the result is a single element: not a stream, not some virtual DOM representation.
  override def renderTodoItem(itemId: Int, initialTodo: TodoItem, $item: Signal[TodoItem]): HtmlElement = {
    val isEditingVar = Var(false) // Example of local state
    dom.console.log(highlightId.now())
    val updateTextObserver = commandObserver.contramap[UpdateText] { updateCommand =>
      isEditingVar.set(false)
      updateCommand
    }
    li(      
      Seq(
        styleAttr("transition: 1s;"),
        background <-- highlightId.signal.combineWith(activeWord).flatMap { case(b, s) =>
          //dom.console.log(itemId, lastId, b, s) 
          (b,s) match {
            case (Some(id), None) => {
              if(itemId == id ) {
                dom.console.log("red")
                dom.console.log(itemId, lastId, b)
                highlightId.set(None)                
                Val("red")                
              } else {
                dom.console.log("white")
                Val("white")
              }
            }          
            case(None, Some(word)) => {
              dom.console.log("active word")
              val temp = $item.map(_.text.toLowerCase.contains(word.toLowerCase()) ).map { b => 
                if(b) {"red"} else {"white"}                
              }
              //Val("red")
              temp
            }
              case(_, _)  => {
                dom.console.log("white")
                Val("white")
                
              }
            }
        }
      ),
      cls <-- $item.map(item => Map("completed" -> item.completed)),
      onDblClick.filter(_ => !isEditingVar.now()).mapTo(true) --> isEditingVar.writer,
      children <-- isEditingVar.signal.map[List[HtmlElement]] {
        case true =>
          renderTextUpdateInput(itemId, $item, updateTextObserver) :: Nil
        case false =>
          List(
            renderCheckboxInput(itemId, $item),
            label(child.text <-- $item.map(_.text)),
            button(cls("destroy"), onClick.mapTo(Delete(itemId)) --> commandObserver)
          )
      }
    )
  }

  def listen(name: Dynamic, value:  Dynamic) = {
    dom.console.log(name, value)
  }


  val pieSpec: String = """
  {
  "$schema": "https://vega.github.io/schema/vega/v5.json",
  "description": "A basic pie chart example.",
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
          },
          {
            "name": "active",
            "value": {},
            "on": [
              {"events": "@wordMarks:mousedown, @wordMarks:touchstart", "update": "pluck(datum, 'text')"},
              {"events": "window:mouseup, window:touchend", "update": "{}"}
            ]
          },
    {"name" : "widthover2", "update": "width / 2"   },
    {"name" : "widthover10", "update": "width/10"   }
    

  ],

  "data": [
    {
      "name": "table",
      "values": [
        {"id": "open", "field": 4},
        {"id": "closed", "field": 6}
      ],
      "transform": [
        {
          "type": "pie",
          "field": "field"     
        }
      ]
    }
  ],

  "scales": [
    {
      "name": "color",
      "type": "ordinal",
      "domain": {"data": "table", "field": "id"},
      "range": {"scheme": "category20"}
    },
    {
      "name": "r",      
      "type": "sqrt",
      "domain": {"data": "table", "field": "field"},
      "zero": true,
      "range": [0, 0]
    }
  ],

  "marks": [
    {
      "type": "arc",
      "from": {"data": "table"},
      "encode": {
        "enter": {
          "fill": {"scale": "color", "field": "id"},
          "x": {"signal": "width / 2"},
          "y": {"signal": "height / 2"}
        },
        "update": {
          "startAngle": {"field": "startAngle"},
          "endAngle": {"field": "endAngle"},
          "padAngle": [{"value": 0}],
          "innerRadius": {"signal": "width / 2.5"},
          "outerRadius": {"signal": "width / 2"},
          "cornerRadius": [{"value": 0}]
        }
      }
    },
       {
      "type": "text",
      "from": {"data": "table"},
      "encode": {
        "enter": {
          "x": {"field": {"group": "width"}, "mult": 0.5},
          "y": {"field": {"group": "height"}, "mult": 0.5},
          "radius": {"scale": "r", "field": "field", "offset": {"signal": "width / 4"}},
          "theta": {"signal": "(datum.startAngle + datum.endAngle)/2"},          
          "text": {"field": "id"}
        }
      }
    }
  ]
}
"""

val wordCloudSpec : String = """{
  "$schema": "https://vega.github.io/schema/vega/v5.json",
  "description": "A word cloud visualization depicting Vega research paper abstracts.",
  "padding": 0,
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
        },
        {
          "name": "active",
          "value": {},
          "on": [
            {"events": "@workdMarks:mousedown, @workdMarks:touchstart", "update": "pluck(datum, 'text')"},
            {"events": "window:mouseup, window:touchend", "update": "NaN"}
          ]
        }
    ],
  "data": [
    {
      "name": "table",
      "values": [
        ""
      ],
      "transform": [
        {
          "type": "countpattern",
          "field": "data",
          "case": "lower",
          "stopwords": ""
        },
        {
          "type": "formula", "as": "angle",
          "expr": "[-45, 0, 45][~~(random() * 3)]"
        },
        {
          "type": "formula", "as": "weight",
          "expr": "if(datum.text=='VEGA', 600, 300)"
        }
      ]
    }
  ],

  "scales": [
    {
      "name": "color",
      "type": "ordinal",
      "domain": {"data": "table", "field": "text"},
      "range": ["#d5a928", "#652c90", "#939597"]
    }
  ],

  "marks": [
    {
      "name": "workdMarks",
      "type": "text",
      "from": {"data": "table"},
      "encode": {
        "enter": {
          "text": {"field": "text"},
          "align": {"value": "center"},
          "baseline": {"value": "alphabetic"},
          "fill": {"scale": "color", "field": "text"}
        },
        "update": {
          "fillOpacity": {"value": 1}
        },
        "hover": {
          "fillOpacity": {"value": 0.5}
        }
      },
      "transform": [
        {
          "name" : "wordMarks",
          "type": "wordcloud",
          "size": [{"signal": "width"},  {"signal": "height"}],
          "text": {"field": "text"},
          "rotate": {"field": "datum.angle"},
          "font": "Helvetica Neue, Arial",
          "fontSize": {"field": "datum.count"},
          "fontWeight": {"field": "datum.weight"},
          "fontSizeRange": [20, 30],
          "padding": 2
        }
      ]
    }
  ]
}
"""  

}

