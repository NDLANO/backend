

final class common$_ {
def args = common_sc.args$
def scriptPath = """common.sc"""
/*<script>*/
import mill._, mill.scalalib._
import $file.shared

object Common extends shared.BaseModule {}

/*</script>*/ /*<generated>*//*</generated>*/
}

object common_sc {
  private var args$opt0 = Option.empty[Array[String]]
  def args$set(args: Array[String]): Unit = {
    args$opt0 = Some(args)
  }
  def args$opt: Option[Array[String]] = args$opt0
  def args$: Array[String] = args$opt.getOrElse {
    sys.error("No arguments passed to this script")
  }

  lazy val script = new common$_

  def main(args: Array[String]): Unit = {
    args$set(args)
    val _ = script.hashCode() // hashCode to clear scalac warning about pure expression in statement position
  }
}

export common_sc.script as `common`

