package controllers

import javax.inject.Inject
import models.CalculationModel
import play.api.mvc.{MessagesAbstractController, MessagesControllerComponents}
import services.SimbaAlias._

class CalculationController @Inject()(calculationModel: CalculationModel, mcc: MessagesControllerComponents) extends MessagesAbstractController(mcc) {

//  def toCalculationsView: PlayAction = Action {
//    Ok(views.html.calculations(calculationModel.getCalculations))
//  }
}
