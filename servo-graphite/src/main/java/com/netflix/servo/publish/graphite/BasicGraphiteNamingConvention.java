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
package com.netflix.servo.publish.graphite;

import com.netflix.servo.Metric;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.TagList;

/**
 * A basic graphite naming convention that handles both "native servo" objects and standard JMX
 * objects pulled out of the JMX registry.
 */
public class BasicGraphiteNamingConvention implements GraphiteNamingConvention {

    private static final String JMX_DOMAIN_KEY = "JmxDomain";

    @Override
    public String getName(Metric metric) {
        MonitorConfig config = metric.getConfig();
        TagList tags = config.getTags();

        Tag domainTag = tags.getTag(JMX_DOMAIN_KEY);
        if(domainTag != null){
           return handleJmxMetric(config, tags);
        }else{
           return cleanupIllegalCharacters(config.getName(), true);
        }
    }

    private String handleJmxMetric(MonitorConfig config, TagList tags){
        String domain = cleanValue(tags.getTag( JMX_DOMAIN_KEY ), true);
        String type = cleanValue(tags.getTag("Jmx.type"), false);
        String name = cleanValue(tags.getTag("Jmx.name"), false);
        String instanceName = cleanValue(tags.getTag("Jmx.instance"), false);
        String fieldName = cleanupIllegalCharacters(config.getName(), false);

        StringBuilder nameBuilder = new StringBuilder();
        nameBuilder.append( domain ).append( "." );
        if(type != null){
            nameBuilder.append( type ).append(".");
        }
        if(instanceName != null){
            nameBuilder.append( instanceName ).append( "." );
        }
        if(name != null){
            nameBuilder.append( name ).append( "." );
        }
        if(fieldName != null){
            nameBuilder.append( fieldName );
        }
        return nameBuilder.toString();
    }

    private String cleanValue(Tag tag, boolean allowPeriodsInName){
        if(tag == null) return null;

        return cleanupIllegalCharacters(tag.getValue(), allowPeriodsInName);
    }

    private String cleanupIllegalCharacters(String s, boolean allowPeriodsInName){
        if(!allowPeriodsInName){
            s = s.replace(".", "_");
        }
        return s.replace(" ", "_");
    }
}
