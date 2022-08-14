/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.iotdb.metrics.predefined.jvm;

import org.apache.iotdb.metrics.AbstractMetricManager;
import org.apache.iotdb.metrics.predefined.IMetricSet;
import org.apache.iotdb.metrics.predefined.PredefinedMetric;
import org.apache.iotdb.metrics.utils.MetricLevel;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;

/** This file is modified from io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics */
public class JvmThreadMetrics implements IMetricSet {
  @Override
  public void bindTo(AbstractMetricManager metricManager) {
    ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

    metricManager.getOrCreateAutoGauge(
        "jvm.threads.peak.threads",
        MetricLevel.IMPORTANT,
        threadBean,
        ThreadMXBean::getPeakThreadCount);

    metricManager.getOrCreateAutoGauge(
        "jvm.threads.daemon.threads",
        MetricLevel.IMPORTANT,
        threadBean,
        ThreadMXBean::getDaemonThreadCount);

    metricManager.getOrCreateAutoGauge(
        "jvm.threads.live.threads",
        MetricLevel.IMPORTANT,
        threadBean,
        ThreadMXBean::getThreadCount);

    try {
      threadBean.getAllThreadIds();
      for (Thread.State state : Thread.State.values()) {
        metricManager.getOrCreateAutoGauge(
            "jvm.threads.states.threads",
            MetricLevel.IMPORTANT,
            threadBean,
            (bean) -> getThreadStateCount(bean, state),
            "state",
            getStateTagValue(state));
      }
    } catch (Error error) {
      // An error will be thrown for unsupported operations
      // e.g. SubstrateVM does not support getAllThreadIds
    }
  }

  // VisibleForTesting
  static long getThreadStateCount(ThreadMXBean threadBean, Thread.State state) {
    return Arrays.stream(threadBean.getThreadInfo(threadBean.getAllThreadIds()))
        .filter(threadInfo -> threadInfo != null && threadInfo.getThreadState() == state)
        .count();
  }

  private static String getStateTagValue(Thread.State state) {
    return state.name().toLowerCase().replace("_", "-");
  }

  @Override
  public PredefinedMetric getType() {
    return PredefinedMetric.JVM;
  }
}
