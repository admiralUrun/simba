package models

import java.util.Calendar
import dao.Dao
import javax.inject.Inject

class CalculationModel @Inject()(dao: Dao) {

  def getCalculations: Seq[Calculation] = {
    val m = Calendar.getInstance
    val s = Calendar.getInstance
    m.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    s.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    dao.getCalculationsOnThisWeek(s.getTime, m.getTime)
  }
}

case class Calculation(description: String, unit: String, artBy: Int, count: Float)