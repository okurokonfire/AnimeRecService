package anilist.recsystem

import  org.scalatest.flatspec.AnyFlatSpec
import  org.scalatest.matchers.should._

class CollectJsonTest extends AnyFlatSpec with Matchers {
    "process user" should "not fail" in {
        anilist.recsystem.ProcessData.processMediaInfo(anilist.recsystem.CollectJsonInfo.collectMediaInfo(1))
    }
}