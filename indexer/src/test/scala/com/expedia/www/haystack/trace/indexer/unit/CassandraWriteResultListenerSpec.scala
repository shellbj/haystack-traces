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

package com.expedia.www.haystack.trace.indexer.unit

import java.util
import java.util.Collections
import java.util.concurrent.Semaphore

import com.codahale.metrics.Timer
import com.datastax.driver.core.{ExecutionInfo, ResultSet, ResultSetFuture}
import com.expedia.www.haystack.trace.indexer.writers.cassandra.CassandraWriteResultListener
import org.scalatest.easymock.EasyMockSugar
import org.scalatest.{FunSpec, Matchers}

class CassandraWriteResultListenerSpec extends FunSpec with Matchers with EasyMockSugar {
  describe("Cassandra Write Listener") {
    it("should run successfully without reporting any warnings") {
      val asyncResult = mock[ResultSetFuture]
      val resultSet = mock[ResultSet]
      val timer = mock[Timer.Context]
      val inflightSemaphore = mock[Semaphore]
      val executionInfo = mock[ExecutionInfo]

      expecting {
        inflightSemaphore.release().once()
        timer.close().once()
        asyncResult.get().andReturn(resultSet).atLeastOnce()
        executionInfo.getWarnings.andReturn(Collections.emptyList()).atLeastOnce()
        resultSet.getExecutionInfo.andReturn(executionInfo).atLeastOnce()
      }
      whenExecuting(asyncResult, resultSet, timer, inflightSemaphore, executionInfo) {
        val listener = new CassandraWriteResultListener(asyncResult, timer, inflightSemaphore)
        listener.run()
      }
    }

    it("should run successfully without throwing any error even if asyncResult has errored") {
      val asyncResult = mock[ResultSetFuture]
      val timer = mock[Timer.Context]
      val inflightSemaphore = mock[Semaphore]

      expecting {
        inflightSemaphore.release().once()
        timer.close().once()
        asyncResult.get().andThrow(new RuntimeException)
      }
      whenExecuting(asyncResult, timer, inflightSemaphore) {
        val listener = new CassandraWriteResultListener(asyncResult, timer, inflightSemaphore)
        listener.run()
      }
    }

    it("should run successfully with warnings reported") {
      val asyncResult = mock[ResultSetFuture]
      val resultSet = mock[ResultSet]
      val timer = mock[Timer.Context]
      val inflightSemaphore = mock[Semaphore]
      val executionInfo = mock[ExecutionInfo]

      val warnings = util.Arrays.asList("warning-1")

      expecting {
        inflightSemaphore.release().once()
        timer.close().once()
        executionInfo.getWarnings.andReturn(warnings).atLeastOnce()
        resultSet.getExecutionInfo.andReturn(executionInfo).atLeastOnce()
        asyncResult.get().andReturn(resultSet).atLeastOnce()
      }
      whenExecuting(asyncResult, resultSet, timer, inflightSemaphore, executionInfo) {
        val listener = new CassandraWriteResultListener(asyncResult, timer, inflightSemaphore)
        listener.run()
      }
    }
  }
}
