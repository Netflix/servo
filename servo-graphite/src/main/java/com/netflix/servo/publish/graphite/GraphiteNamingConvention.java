/**
 * Copyright 2013 Netflix, Inc.
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
package com.netflix.servo.publish.graphite;

import com.netflix.servo.Metric;

/**
 * We want to allow the user to override the default graphite naming convention to massage the objects into
 * the right shape for their graphite setup. Naming conventions could also be applied to other observers
 * such as the file observer in the future.
 */
public interface GraphiteNamingConvention {

    String getName(Metric metric);
}
