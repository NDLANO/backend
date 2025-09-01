object Logger {
  def info(msg: String): Unit  = println(s"[ℹ️] $msg")
  def warn(msg: String): Unit  = println(s"[⚠️] $msg")
  def error(msg: String): Unit = println(s"[❌] $msg")
}
