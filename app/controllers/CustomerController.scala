package controllers

import javax.inject._
import models.{Customer, CustomerModel}
import play.api.data.Forms._
import play.api.mvc._
import play.api.data._

@Singleton
class CustomerController @Inject()(cM: CustomerModel, cc: MessagesControllerComponents) extends MessagesAbstractController(cc) {

  private val customerForm = Form(
    mapping(
      "id" -> ignored(None: Option[String]),
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

  def search(s:String) = TODO // TODO

  def toCustomersListPage = Action { implicit request =>
    Ok(views.html.customersView(cM.getAllTableRows))
  }
  def toCreateCustomerPage() = Action { implicit request =>
    Ok(views.html.createCustomerView(customerForm))
  }
  def toEditCustomerPage(id: String) = Action { implicit request =>
    val customer = cM.findByID(id)
    Ok(views.html.editCustomerView(id, customerForm.fill(customer), customer.firstName))
  }

  def update() = Action { implicit request =>
    customerForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.editCustomerView(formWithErrors.data("id"), formWithErrors, formWithErrors.data("firstName"))),
      customer => {
        cM.editCustomer(customer)
        customerListPage.flashing("success" -> "Клієнта змінено, мяу")
      }
    )
  }
  def createCustomer() = Action { implicit request =>
    customerForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.createCustomerView(formWithErrors)),
      newCustomer => {
        cM.insert(newCustomer)
        customerListPage.flashing("success" -> "Клієнта додано, мяу")
      }
    )
  }
}
