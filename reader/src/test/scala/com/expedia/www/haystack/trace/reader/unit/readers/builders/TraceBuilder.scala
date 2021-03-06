package com.expedia.www.haystack.trace.reader.unit.readers.builders

import com.expedia.open.tracing.api.Trace
import com.expedia.open.tracing.{Log, Span, Tag}

import scala.collection.JavaConversions._

// helper to create various types of traces for unit testing
trait TraceBuilder {
  val startTimestamp = 150000000000l
  val traceId = "traceId"

  protected def toTrace(spans: Span*): Trace = Trace.newBuilder().setTraceId(traceId).addAllChildSpans(spans).build

  protected def createServerSpanTags(start: Long, end: Long) = List(
    Log.newBuilder()
      .setTimestamp(start)
      .addFields(Tag.newBuilder().setKey("event").setVStr("sr").build())
      .build(),
    Log.newBuilder()
      .setTimestamp(end)
      .addFields(Tag.newBuilder().setKey("event").setVStr("ss").build())
      .build()
  )

  protected def createClientSpanTags(start: Long, end: Long) = List(
    Log.newBuilder()
      .setTimestamp(start)
      .addFields(Tag.newBuilder().setKey("event").setVStr("cs").build())
      .build(),
    Log.newBuilder()
      .setTimestamp(end)
      .addFields(Tag.newBuilder().setKey("event").setVStr("cr").build())
      .build()
  )

  protected def getSpan(trace: Trace, spanId: String): Span = trace.getChildSpansList.find(_.getSpanId == spanId).get

}
