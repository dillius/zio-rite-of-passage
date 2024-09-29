package com.rockthejvm.reviewboard.core

import com.raquo.laminar.api.L.{*, given}
import sttp.client3.*
import sttp.client3.impl.zio.FetchZioBackend
import sttp.tapir.*
import sttp.tapir.client.sttp.SttpClientInterpreter
import zio.*
import com.rockthejvm.reviewboard.config.*

object ZJS {
//  def backendCall[A](clientFun: BackendClient => Task[A]) =
//    ZIO.serviceWithZIO[BackendClient](clientFun)
  def useBackend = ZIO.serviceWithZIO[BackendClient]

  extension [E <: Throwable, A](zio: ZIO[BackendClient, E, A]) {
    def emitTo(eventBus: EventBus[A]) =
      Unsafe.unsafe { implicit unsafe =>
        Runtime.default.unsafe.fork(
          zio
            .tap(value => ZIO.attempt(eventBus.emit(value)))
            .provide(BackendClientLive.configuredLayer)
        )
      }

    def toEventStream: EventStream[A] = {
      val bus = EventBus[A]()
      emitTo(bus)
      bus.events
    }

    def runJs =
      Unsafe.unsafe(implicit unsafe =>
        Runtime.default.unsafe.runToFuture(zio.provide(BackendClientLive.configuredLayer))
      )
  }

  extension [I, E <: Throwable, O](endpoint: Endpoint[Unit, I, E, O, Any])
    def apply(payload: I): Task[O] =
      ZIO
        .serviceWithZIO[BackendClient](_.endpointRequestZIO(endpoint)(payload))
        .provide(BackendClientLive.configuredLayer)
}
