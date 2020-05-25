package controllers

import javax.inject._
import models.{Customer, CustomerModel}
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._

@Singleton
class CustomerController @Inject()(customerModel: CustomerModel, mcc: MessagesControllerComponents) extends MessagesAbstractController(mcc) {
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
  private val customerListPage = Redirect(routes.CustomerController.toCustomersListPage("", ""))

  def toCustomersListPage(search: String, select: String): PlayAction = Action { implicit request =>
    val rows = if(search.isEmpty) customerModel.getAllTableRows else customerModel.getAllTableRowsWhere(search)
    Ok(views.html.customers(rows, search))
  }

  def toCreateCustomerPage: PlayAction = Action { implicit request =>
    Ok(views.html.createCustomer(customerForm))
  }

  def toEditCustomerPage(id: Int): PlayAction = Action { implicit request =>
    val customer = customerModel.findByID(id)
    Ok(views.html.editCustomer(id, customerForm.fill(customer), customer.firstName))
  }

  def update(id: Int): PlayAction = Action { implicit request =>
    customerForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.editCustomer(id, formWithErrors, formWithErrors.data("firstName"))),
      customer => {
        resultWithFlash(customerModel.editCustomer(id, customer),"Клієнта змінено, мяу")
      }
    )
  }

  def createCustomer: PlayAction = Action { implicit request =>
    customerForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.createCustomer(formWithErrors)),
      newCustomer => {
        resultWithFlash(customerModel.insert(newCustomer),"Клієнта додано, мяу")
      }
    )
  }

  private def resultWithFlash(modelResult: Boolean, successFlash: String, errorFlash: String = "Щось пішло не так ;("): Result = {
    if(modelResult) customerListPage.flashing("success" -> successFlash)
    else customerListPage.flashing("error" -> errorFlash)
  }
}
