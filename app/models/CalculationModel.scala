package models

import java.util.Calendar
import javax.inject.Inject
import zio.Task
import dao.Dao



class CalculationModel @Inject()(dao: Dao) {

  def getCalculations: Task[Seq[Calculation]] = {
    val m = Calendar.getInstance
    val s = Calendar.getInstance
    m.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    s.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    dao.getCalculationsOnThisWeek(s.getTime, m.getTime)
  }
}

case class Calculation(description: String, unit: String, artBy: Int, count: Float)