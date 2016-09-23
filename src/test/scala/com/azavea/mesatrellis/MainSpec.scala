package com.azavea.mesatrellis

import org.scalatest._

class MainSpec extends FunSpec with Matchers {
  describe("Main") {
    it("should have correct hello sentence") {
      HelloWorld.helloSentence should be ("Hello GeoTrellis")
    }
  }
}
