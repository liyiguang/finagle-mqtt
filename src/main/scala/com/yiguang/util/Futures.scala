package com.yiguang.util

import com.twitter.concurrent.{ThreadPoolScheduler, Scheduler}
import com.twitter.util.{Await, Return, Promise, Future}

/**
 * Created by yigli on 14-11-28.
 */
object Futures {

  private[this] val scheduler: Scheduler = new ThreadPoolScheduler("SynToAsyn")

  /**
   * because it will be confused, so not privde as implicit
   * @param f
   * @tparam R
   * @return
   */
  def asyn[R](f: => R): Future[R] = {

    val p = Promise[R]()
    scheduler.submit(new Runnable {
      def run(): Unit = {
        try {
          val r = f
          p.setValue(r)
        }
        catch {
          case e =>
            p.setException(e)
        }
      }
    })
    p
  }
}

object Test extends App {
  import Futures.asyn

  def costtime_call1 = {
    Thread.sleep(2000)
    println("finishe costtime_call1")
  }

  def costtime_call2(p:String):String = {
    Thread.sleep(2000)
    println("finishe costtime_call2 with "+ p)

    return "OK"
  }

  val f1 = asyn(costtime_call1)

  val f2 = asyn{
    costtime_call2("hello")
  }

  f2.onSuccess(x=>println("Returned:"+x))


  Await.result(f1)
  val ret = Await.result(f2)

  println("WaitResult:"+ret)


  Thread.sleep(1000)

  Future.value()

}


