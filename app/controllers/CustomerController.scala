package controllers

import javax.inject._
import models._
import play.api.data.Forms._
import play.api.data._
import play.api.libs.functional.syntax.unlift
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json}
import play.api.mvc._
import services.SimbaAlias._

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
      "instagram" -> optional(text),
      "preferences" -> optional(text),
      "notes" -> optional(text),
      "addresses" -> list(text)
    )(CustomerInput.apply)(CustomerInput.unapply)
  )
  private val toCustomerListPage: Call = routes.CustomerController.toCustomersListPage("")
  private val toOrderCreatePage: Call = routes.OrderController.toOrderCreatePage()
  private val customerListPage: Result = Redirect(toCustomerListPage)

  private implicit val customerWriter: JSONWrites[Customer] = (
    (JsPath \ "id").writeNullable[ID] and
      (JsPath \ "firstName").write[String] and
      (JsPath \ "lastName").writeNullable[String] and
      (JsPath \ "phone").write[String] and
      (JsPath \ "phoneNote").writeNullable[String] and
      (JsPath \ "phone2").writeNullable[String] and
      (JsPath \ "phoneNote2").writeNullable[String] and
      (JsPath \ "instagram").writeNullable[String] and
      (JsPath \ "preferences").writeNullable[String] and
      (JsPath \ "notes").writeNullable[String]
    ) (unlift(Customer.unapply))

  private implicit val addressWriter: JSONWrites[Address] = (
    (JsPath \ "id").writeNullable[ID] and
      (JsPath \ "customerId").writeNullable[ID] and
      (JsPath \ "city").write[String] and
      (JsPath \ "residentialComplex").writeNullable[String] and
      (JsPath \ "address").write[String] and
      (JsPath \ "entrance").writeNullable[String] and
      (JsPath \ "floor").writeNullable[String] and
      (JsPath \ "flat").writeNullable[String] and
      (JsPath \ "notesForCourier").writeNullable[String]
    ) (unlift(Address.unapply))

  private implicit val customerAddressesToJsonWriter: JSONWrites[CustomerAddressesForJson] = (
    (JsPath \ "customer").write[Customer] and
      (JsPath \ "addresses").write[Seq[Address]]
    ) (unlift(CustomerAddressesForJson.unapply))

  def toCustomersListPage(search: String): PlayAction = Action { implicit request =>
    val rows = if (search.isEmpty) customerModel.getAllCustomerTableRows else customerModel.getAllCustomerTableRowsLike(search)

    Ok(views.html.customers(rows, search))
  }

  def getCustomersForOrderSearch(search: String): PlayAction = Action {
    Ok(Json.toJson(customerModel.getDataForJsonToDisplayInOrder(search).unsafeRunSync()))
  }

  def getCustomersForOrder(id: Int): PlayAction = Action {
    Ok(Json.toJson(customerModel.getDataForJsonToDisplayInOrderBy(id).unsafeRunSync()))
  }

  def toCreateCustomerPage: PlayAction = Action { implicit request =>
    Ok(views.html.createCustomer(customerForm, routes.CustomerController.createCustomer(), toCustomerListPage))
  }

  def toCreateCustomerPageForOrder: PlayAction = Action { implicit request =>
    Ok(views.html.createCustomer(customerForm, routes.CustomerController.createCustomerThenToOrder(), toOrderCreatePage))
  }

  def toEditCustomerPage(id: Int): PlayAction = Action { implicit request =>
    val customer = customerModel.findBy(id)
    Ok(views.html.editCustomer(id, customerForm.fill(customer), customer.firstName))
  }

  def update(id: Int): PlayAction = Action { implicit request =>
    customerForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.editCustomer(id, formWithErrors, formWithErrors.data("firstName"))),
      customer => {
        resultWithFlash(customerListPage, customerModel.editCustomer(id, customer), "Клієнта змінено, мяу")
      }
    )
  }

  def createCustomer: PlayAction = Action { implicit request =>
    customerForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.createCustomer(formWithErrors, routes.CustomerController.createCustomer(), toCustomerListPage)),
      newCustomer => {
        resultWithFlash(customerListPage, customerModel.insert(newCustomer)._1, "Клієнта додано, мяу")
      }
    )
  }

  def createCustomerThenToOrder: PlayAction = Action { implicit request =>
    customerForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.createCustomer(formWithErrors, routes.CustomerController.createCustomerThenToOrder(), toOrderCreatePage)),
      newCustomer => {
        val modelResponse: (Boolean, Int) = customerModel.insert(newCustomer)
        resultWithFlash(Redirect(routes.OrderController.toOrderCreatePageWithCustomerId(modelResponse._2)), modelResponse._1, "Клієнта додано, мяу")
      }
    )
  }

  private def resultWithFlash(result: Result, modelResponse: Boolean, successFlash: String, errorFlash: String = "Щось пішло не так ;("): Result = {
    if (modelResponse) result.flashing("success" -> successFlash)
    else result.flashing("error" -> errorFlash)
  }
}
