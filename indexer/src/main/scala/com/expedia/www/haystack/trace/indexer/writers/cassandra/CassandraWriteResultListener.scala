/*
 *  Copyright 2017 Expedia, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.expedia.www.haystack.trace.indexer.writers.cassandra

import java.util.concurrent.Semaphore

import com.codahale.metrics.{Meter, Timer}
import com.datastax.driver.core.ResultSetFuture
import com.expedia.www.haystack.trace.indexer.metrics.{AppMetricNames, MetricsSupport}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConversions._

object CassandraWriteResultListener extends MetricsSupport {
  protected val LOGGER: Logger = LoggerFactory.getLogger(CassandraWriteResultListener.getClass)
  protected val writeFailures: Meter = metricRegistry.meter(AppMetricNames.CASSANDRA_WRITE_FAILURE)
  protected val writeWarnings: Meter = metricRegistry.meter(AppMetricNames.CASSANDRA_WRITE_WARNINGS)
}

class CassandraWriteResultListener(asyncResult: ResultSetFuture,
                                   timer: Timer.Context,
                                   inflightRequestsSemaphore: Semaphore) extends Runnable {

  import CassandraWriteResultListener._

  /**
    * this is invoked when the cassandra aysnc write completes.
    * We measure the time write operation takes and records any warnings or errors
    */
  override def run(): Unit = {
    inflightRequestsSemaphore.release()

    try {
      timer.close()

      if (asyncResult.get() != null &&
        asyncResult.get().getExecutionInfo != null &&
        asyncResult.get().getExecutionInfo.getWarnings != null &&
        asyncResult.get().getExecutionInfo.getWarnings.nonEmpty) {
        LOGGER.warn(s"Warning received in cassandra writes {}", asyncResult.get().getExecutionInfo.getWarnings.toList.mkString(","))
        writeWarnings.mark(asyncResult.get.getExecutionInfo.getWarnings.size())
      }
    } catch {
      case ex: Exception =>
        LOGGER.error("Fail to write the record to cassandra with exception", ex)
        writeFailures.mark()
    }
  }
}
