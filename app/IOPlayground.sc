import cats.effect.IO
import cats.implicits._

(IO(println("Done")) *>
IO(throw new Error("Opps!"))).unsafeRunAsyncAndForget()
