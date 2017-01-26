package com.netflix.servo.monitor;


import com.netflix.servo.annotations.*;
import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.TagList;

public class SuperClassWithMonitors {

    @com.netflix.servo.annotations.Monitor
    public Integer monitor1;
    private Integer monitor2;

    public Integer getMonitor1() {
        return monitor1;
    }

    public void setMonitor1(Integer monitor1) {
        this.monitor1 = monitor1;
    }

    @com.netflix.servo.annotations.Monitor
    public Integer getMonitor2() {
        return monitor2;
    }

    public void setMonitor2(Integer monitor2) {
        this.monitor2 = monitor2;
    }

    public static class ChildClassWithMonitors extends SuperClassWithMonitors {

        @com.netflix.servo.annotations.Monitor
        public Integer monitor3;
        private Integer monitor4;

        @MonitorTags
        private TagList tags;

        public ChildClassWithMonitors() {
            this.tags = BasicTagList.of("tag1", "tag2");
        }

        public Integer getMonitor3() {
            return monitor3;
        }

        public void setMonitor3(Integer monitor3) {
            this.monitor3 = monitor3;
        }

        @com.netflix.servo.annotations.Monitor
        public Integer getMonitor4() {
            return monitor4;
        }

        public void setMonitor4(Integer monitor4) {
            this.monitor4 = monitor4;
        }

        public TagList getTags() {
            return tags;
        }

        public void setTags(TagList tags) {
            this.tags = tags;
        }
    }
}
