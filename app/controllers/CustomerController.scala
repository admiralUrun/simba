package controllers

import javax.inject._
import models.{Customer, CustomerModel, DoobieModel}
import play.api.data.Forms._
import play.api.mvc._
import play.api.data._

@Singleton
class CustomerController @Inject()(cM: CustomerModel, cc: MessagesControllerComponents) extends MessagesAbstractController(cc) {
type PlayAction = Action[AnyContent]
  private val customerForm = Form(
    mapping(
      "id" -> ignored(None: Option[Int]),
      "firstName" -> nonEmptyText,
      "lastName" -> optional(text),
      "phone" -> nonEmptyText,
      "phoneNote" -> optional(text),
      "phone2" -> optional(text),
      "phoneNote2" -> optional(text),
      "city" -> nonEmptyText,
      "address" -> nonEmptyText,
      "flat" -> optional(text),
      "entrance" -> optional(text),
      "floor" -> optional(text),
      "instagram" -> optional(text),
      "preferences" -> optional(text),
      "notes" -> optional(text)
    )(Customer.apply)(Customer.unapply)
  )
  private val customerListPage = Redirect(routes.CustomerController.toCustomersListPage())

  def search(s: String): PlayAction = TODO // TODO

  def toCustomersListPage: PlayAction = Action { implicit request =>
    Ok(views.html.customersView(cM.getAllTableRows))
  }
  def toCreateCustomerPage: PlayAction = Action { implicit request =>
    Ok(views.html.createCustomerView(customerForm))
  }
  def toEditCustomerPage(id: Int): PlayAction = Action { implicit request =>
    val customer = cM.findByID(id)
    Ok(views.html.editCustomerView(id, customerForm.fill(customer), customer.firstName))
  }

  def update(id: Int): PlayAction = Action { implicit request =>
    customerForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.editCustomerView(id, formWithErrors, formWithErrors.data("firstName"))),
      customer => {
        if(cM.editCustomer(id, customer)) customerListPage.flashing("success" -> "Клієнта змінено, мяу")
        else customerListPage.flashing("error" -> "Щось пішло не так ;(")
      }
    )
  }
  def createCustomer: PlayAction = Action { implicit request =>
    customerForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.createCustomerView(formWithErrors)),
      newCustomer => {
        if(cM.insert(newCustomer)) customerListPage.flashing("success" -> "Клієнта додано, мяу")
        else customerListPage.flashing("error" -> "Щось пішло не так ;(")
      }
    )
  }
}
