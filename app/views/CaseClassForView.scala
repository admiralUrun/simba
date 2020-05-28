package views

case class OrderMenuItem(title: String, value: String, cost: Int)
case class OrderMenu(title:String, menuItems:List[OrderMenuItem])
