package services

import scala.language.higherKinds
import play.api.mvc._
import zio.{Runtime, Task}

object SimbaAction {
	private val runtime = Runtime.global
	implicit class ActionBuilderOps[+R[_], B](actionBuilder: ActionBuilder[R, B]) {

		def zio[E, A](bp: BodyParser[A])(body: R[A] => Task[Result]): Action[A] = actionBuilder(bp) async { request =>
			runtime.unsafeRunToFuture(body(request))
		}

	}
}
