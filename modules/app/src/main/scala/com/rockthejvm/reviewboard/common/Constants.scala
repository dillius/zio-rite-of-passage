package com.rockthejvm.reviewboard.common

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.codecs.*
import org.scalajs.dom

import scala.scalajs.js.annotation.*
import scala.scalajs.js

object Constants {

  @js.native
  @JSImport("/static/img/fiery-lava 128x128.png", JSImport.Default)
  val logoImage: String = js.native

  @js.native
  @JSImport("/static/img/generic_company.png", JSImport.Default)
  val companyLogoPlaceholder: String = js.native

}
