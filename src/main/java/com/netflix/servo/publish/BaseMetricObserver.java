/*
 * #%L
 * servo
 * %%
 * Copyright (C) 2011 - 2012 Netflix
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
/*
 * Copyright (c) 2012. Netflix, Inc.
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
 */

package com.netflix.servo.publish;

import com.google.common.base.Preconditions;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.netflix.servo.annotations.MonitorId;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: gorzell
 * Date: 1/3/12
 * Time: 11:33 AM
 */
public abstract class BaseMetricObserver implements MetricObserver {
    @MonitorId
    protected final String name;
    @Monitor(name="UpdateCount", type= DataSourceType.COUNTER,
             description="Total number of times update has been called on "
                        +"the wrapped observer.")
    protected final AtomicInteger updateCount = new AtomicInteger(0);
    @Monitor(name="UpdateFailureCount", type= DataSourceType.COUNTER,
             description="Number of times the update call on the wrapped "
                        +"observer failed with an exception.")
    protected final AtomicInteger failedUpdateCount = new AtomicInteger(0);

    public BaseMetricObserver(String name) {
        this.name = Preconditions.checkNotNull(name);
    }

    public abstract void update(List<Metric> metrics);
    
    public String getName(){
        return this.name;
    }
}
