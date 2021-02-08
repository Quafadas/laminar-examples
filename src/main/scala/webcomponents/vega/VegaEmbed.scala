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
import scala.concurrent.Future
import scala.concurrent.Promise



object VegaEmbed {  

        @js.native
        @JSImport("vega-embed", JSImport.Default)
        def embed(clz : String, spec : js.Dynamic, opts : js.Dynamic) : js.Promise[js.Dynamic] = js.native

}


