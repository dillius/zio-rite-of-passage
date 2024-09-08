package com.rockthejvm.reviewboard.pages

import com.raquo.laminar.api.L.{*, given}
import com.rockthejvm.reviewboard.common.Constants
import com.rockthejvm.reviewboard.components.*
import com.rockthejvm.reviewboard.domain.data.*
import org.scalajs.dom

object CompaniesPage {

  val dummyCompnay = Company(
    1L,
    "dummy-company",
    "Simple Company",
    "http://dummy.com",
    Some("Anywhere"),
    Some("On Mars"),
    Some("space travel"),
    None,
    List("space", "scala")
  )

  def apply() =
    sectionTag(
      cls := "section-1",
      div(
        cls := "container company-list-hero",
        h1(
          cls := "company-list-title",
          "Rock the JVM Companies Board"
        )
      ),
      div(
        cls := "container",
        div(
          cls := "row jvm-recent-companies-body",
          div(
            cls := "col-lg-4",
            div("TODO filter panel here")
          ),
          div(
            cls := "col-lg-8",
            renderCompany(dummyCompnay),
            renderCompany(dummyCompnay)
          )
        )
      )
    )

  private def renderCompanyPicture(company: Company) =
    img(
      cls := "img-fluid",
      src := company.image.getOrElse(Constants.companyLogoPlaceholder),
      alt := company.name
    )

  private def renderAction(company: Company) =
    div(
      cls := "jvm-recent-companies-card-btn-apply",
      a(
        href   := company.url,
        target := "blank",
        button(
          `type` := "button",
          cls    := "btn btn-danger rock-action-btn",
          "Website"
        )
      )
    )

  private def renderDetail(icon: String, value: String) =
    div(
      cls := "company-detail",
      i(cls := s"fa fa-$icon company-detail-icon"),
      p(
        cls := "company-detail-value",
        value
      )
    )

  private def fullLocationString(company: Company): String =
    (company.location, company.country) match {
      case (Some(l), Some(c)) => s"$l, $c"
      case (Some(l), None)    => l
      case (None, Some(c))    => c
      case _                  => "N/A"
    }

  private def renderOverview(company: Company) = {
    div(
      cls := "company-summary",
      renderDetail("location-dot", fullLocationString(company)),
      renderDetail("tags", company.tags.mkString(", "))
    )
  }

  def renderCompany(company: Company) =
    div(
      cls := "jvm-recent-companies-cards",
      div(
        cls := "jvm-recent-companies-card-img",
        renderCompanyPicture(company)
      ),
      div(
        cls := "jvm-recent-companies-card-contents",
        h5(
          Anchors.renderNavLink(
            company.name,
            s"/company/${company.id}",
            "company-title-link"
          )
        ),
        renderOverview(company)
      ),
      renderAction(company)
    )
}
