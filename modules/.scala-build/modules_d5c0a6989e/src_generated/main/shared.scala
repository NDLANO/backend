

final class shared$_ {
def args = shared_sc.args$
def scriptPath = """shared.sc"""
/*<script>*/
import mill._, mill.scalalib._

trait BaseModule extends ScalaModule {
  def scalaVersion: String = "2.13.16"
}

/*</script>*/ /*<generated>*//*</generated>*/
}

object shared_sc {
  private var args$opt0 = Option.empty[Array[String]]
  def args$set(args: Array[String]): Unit = {
    args$opt0 = Some(args)
  }
  def args$opt: Option[Array[String]] = args$opt0
  def args$: Array[String] = args$opt.getOrElse {
    sys.error("No arguments passed to this script")
  }

  lazy val script = new shared$_

  def main(args: Array[String]): Unit = {
    args$set(args)
    val _ = script.hashCode() // hashCode to clear scalac warning about pure expression in statement position
  }
}

export shared_sc.script as `shared`

