package webcomponents.vega

import com.raquo.domtypes.generic.codecs.StringAsIsCodec
import com.raquo.laminar.api.L._
import com.raquo.laminar.builders.HtmlTag
import com.raquo.laminar.keys.{ReactiveHtmlAttr, ReactiveProp, ReactiveStyle}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.JSON






@js.native
@JSImport("vega-view", JSImport.Namespace)
class VegaView(parsedSpec : js.Dynamic, config : js.Dynamic) extends js.Object {  

    def runAsync(): Unit = js.native
        
    def data(s: String, j: js.Dynamic) : Unit = js.native
    
}

