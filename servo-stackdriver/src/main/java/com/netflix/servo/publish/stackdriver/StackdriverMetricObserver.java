/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.servo.publish.stackdriver;

import com.netflix.servo.Metric;
import com.netflix.servo.publish.BaseMetricObserver;
import com.stackdriver.api.custommetrics.CustomMetricsMessage;
import com.stackdriver.api.custommetrics.CustomMetricsPoster;
import com.stackdriver.api.custommetrics.DataPoint;
import com.stackdriver.api.custommetrics.InstanceDataPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Ben Fagin
 * @version 2014-03-28
 */
public class StackdriverMetricObserver extends BaseMetricObserver {
	private static final Logger log = LoggerFactory.getLogger(StackdriverMetricObserver.class);
	private final CustomMetricsPoster client;
	private final String instanceID;

	public StackdriverMetricObserver(String name, CustomMetricsPoster client) {
		this(name, null, client);
	}

	public StackdriverMetricObserver(String name, String instanceID, CustomMetricsPoster client) {
		super(name);
		this.client = checkNotNull(client);

		if (instanceID != null) {
			instanceID = instanceID.trim();
			this.instanceID = instanceID.isEmpty() ? null : instanceID;
		} else {
			this.instanceID = null;
		}
	}

	@Override
	public void updateImpl(List<Metric> metrics) {
		CustomMetricsMessage request = new CustomMetricsMessage();

		for (Metric metric : metrics) {
			DataPoint data = convert(metric);

			if (data != null) {
				request.addDataPoint(data);
			}
		}

		try {
			client.sendMetrics(request);
		} catch (Exception ex) {
			log.error("Error while publishing metrics to Stackdriver.", ex);
		}
	}

	private DataPoint convert(Metric metric) {
		final String metricName = metric.getConfig().getName();
		final Date metricTime = new Date(metric.getTimestamp());
		final double metricValue;

		if (metric.hasNumberValue()) {
			metricValue = metric.getNumberValue().doubleValue();
		} else {
			final String value = String.valueOf(metric.getValue());

			// we try to convert boolean metrics to 0.0 / 1.0
			if ("true".equals(value)) {
				metricValue = 1;
			} else if ("false".equals(value)) {
				metricValue = 0;
			}

			// otherwise just parse out the value
			else {
				try {
					metricValue = Double.parseDouble(value);
				} catch (NumberFormatException ex) {
					log.warn("unable to parse metric value for metric '{}'", metricName, ex);
					return null;
				}
			}
		}

		if (instanceID != null) {
			return new InstanceDataPoint(metricName, metricValue, metricTime, instanceID);
		} else {
			return new DataPoint(metricName, metricValue, metricTime);
		}
	}
}
