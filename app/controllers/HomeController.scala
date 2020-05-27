package controllers

import javax.inject._
import models.ImporterModel
import play.api.http.MimeTypes
import play.api.mvc._
import play.api.routing.JavaScriptReverseRouter
/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

//  def javascriptRoutes: Action[AnyContent] = Action { implicit request =>
//    Ok(JavaScriptReverseRouter("jsRoutes")(
//      routes.javascript.CustomerController.getCustomersForOrderSearch)
//    ).as(MimeTypes.JAVASCRIPT)
//  }
  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = Action {
    Ok(views.html.index())
  }

  def importFromCVS() = Action { implicit request =>
    val importer = new ImporterModel
    importer.importCustomersFromCSV
    Ok(views.html.index())
  }

}
