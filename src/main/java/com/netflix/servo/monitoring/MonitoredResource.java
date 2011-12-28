/*
 * Copyright (c) 2011. Netflix, Inc.
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
package com.netflix.servo.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.openmbean.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Wraps a {@link Monitor} annotated {@link Object} w/ a {@link DynamicMBean}
 * Instruments the wrapped object as a <tt>MBean</tt>
 *
 * @author gkim
 */
class MonitoredResource implements DynamicMBean {

    enum Attr {
        Name,
        Type,
        Description,
        MinThreshold,
        MaxThreshold,
        ExpectedValue,
        CurrentValue,
        ObjectName(true);

        //Filter out from item names
        private final boolean _shouldFilterOut;

        Attr() {
            this(false);
        }

        Attr(boolean filterOut) {
            _shouldFilterOut = filterOut;
        }

        boolean shouldFilterOut() {
            return _shouldFilterOut;
        }

        static Attr toAttr(String str) {
            for (Attr attr : Attr.values()) {
                if (attr.name().equalsIgnoreCase(str)) {
                    return attr;
                }
            }
            return null;
        }

        static String[] getItemNames() {
            ArrayList<String> list = new ArrayList<String>();
            for (Attr attr : Attr.values()) {
                if (!attr.shouldFilterOut()) {
                    list.add(attr.name());
                }
            }
            return list.toArray(new String[list.size()]);
        }
    }

    public static final String ITEM_NAMES[] = Attr.getItemNames();


    private static final String TYPE_NAME = "DataSource";
    private static final String TYPE_DESC =
            "DataSource type as an open MBean CompositeType";

    private final Object _resourceToMonitor;

    //The string representation of the field {@link MonitorId} annotates
    private String _displayName;
    private ObjectName _objectName;

    private MBeanAttributeInfo[] _attrInfos;

    private final Map<String, MonitoredEntry> _nameToAttrMap =
            new HashMap<String, MonitoredEntry>();

    private static final Logger logger = LoggerFactory.getLogger(MonitoredResource.class);

    MonitoredResource(MonitorRegistry.Namespace namespace, Object instance) {

        if (instance == null) {
            throw new IllegalArgumentException("Can't wrap a null instance!");
        }

        _resourceToMonitor = instance;

        try {
            logger.info("Registering object to be monitored: " + instance.getClass());

            processAnnotations();

            _objectName = createObjectName(namespace);

            _attrInfos = new MBeanAttributeInfo[_nameToAttrMap.size()];
            int i = 0;

            logger.info("DataSources are: ");
            MBeanAttributeInfo info = null;
            for (MonitoredEntry entry : _nameToAttrMap.values()) {
                info = entry.getInfo();
                _attrInfos[i++] = info;
                logger.info("\t[ " + info.getName()
                        + ": "
                        + info.getType()
                        + "]");
            }
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public MBeanInfo getMBeanInfo() {
        return new MBeanInfo(getResourceObject().getClass().getCanonicalName(),
                TYPE_DESC,
                _attrInfos,
                null,
                null,
                null);
    }

    /**
     * {@inheritDoc}
     */
    public Object getAttribute(String name) {
        Attribute attr = getAttributeByName(name);
        if (attr != null) {
            return attr.getValue();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public AttributeList getAttributes(String[] names) {
        AttributeList attrList = new AttributeList();
        for (String name : names) {
            Attribute attr = getAttributeByName(name);
            if (attr != null) {
                attrList.add(attr);
            } else {
                logger.warn("Unable to find Attribute by following name: " + name);
            }
        }
        return attrList;
    }


    /**
     * Look for our annotations on fields / methods and process them.
     */
    private void processAnnotations() {
        try {
            //traverse class hierarchy and find all annotated fields
            for (Class<?> cls = getResourceObject().getClass(); cls != null; cls = cls
                    .getSuperclass()) {

                //If we are processing jre classes, we've gone to up - so exit
                if (cls.getName().startsWith("java.")) {
                    break;
                }
                Field[] fields = cls.getDeclaredFields();
                for (Field field : fields) {
                    try {
                        Monitor monitor = field.getAnnotation(Monitor.class);
                        if (monitor != null) {
                            _nameToAttrMap.put(monitor.dataSourceName(),
                                    new MonitoredEntry(monitor, field));
                            logger.debug("@Monitor found on field: "
                                    + field.getName() + ": "
                                    + monitor.dataSourceName());
                        }
                        MonitorId monitorId = field
                                .getAnnotation(MonitorId.class);
                        if (monitorId != null) {
                            if (!field.isAccessible()) {
                                field.setAccessible(true);
                            }
                            Object fieldValue = field.get(_resourceToMonitor);
                            if (fieldValue != null) {
                                _displayName = fieldValue.toString();
                                logger.debug("@MonitorId found on field: "
                                        + field.getName());
                            } else {
                                logger.debug("@MonitorId on "
                                        + field.getName() + ": was null!");
                            }
                        }
                    }
                    // It is possible that the declared annotation type is not
                    // present in the classpath, ignore that annotation then
                    catch (TypeNotPresentException e) {
                        logger.warn("The annotation class cannot be loaded while navigating class hierarchy. This may be okay :",
                                e);
                    }
                }
            }
        } catch (OpenDataException e) {
            logger.error("", e);
        } catch (IllegalAccessException e) {
            logger.error("", e);
        }

        try {
            List<Method> methods = new ArrayList<Method>(
                    Arrays.asList(getResourceObject().getClass().getMethods()));
            //Remove Object methods for better efficiency.
            methods.removeAll(Arrays.asList(Object.class.getMethods()));

            for (Method method : methods) {
                //does method have @Monitor annotation?
                Monitor monitor = method.getAnnotation(Monitor.class);
                if (monitor != null) {
                    if (method.getParameterTypes().length > 0 ||
                            method.getReturnType() == java.lang.Void.TYPE) {
                        logger.warn("@Monitor annotated on illegal method: " +
                                method.getName() +
                                " - either returns void or takes parameters");
                        continue;
                    }

                    logger.debug("@Monitor found on method: " + method.getName()
                            + ":  " + monitor.dataSourceName());
                    MonitoredEntry existingEntry = _nameToAttrMap.get(monitor.dataSourceName());
                    if (existingEntry != null) {
                        logger.warn("Overriding existing attribute named:  " +
                                monitor.dataSourceName());
                    }
                    _nameToAttrMap.put(monitor.dataSourceName(), new MonitoredEntry(monitor, method));
                }
            }
        } catch (OpenDataException e) {
            logger.error("", e);
        }
    }

    /**
     * Get {@link Attribute} by data source name
     */
    private Attribute getAttributeByName(String name) {
        Attribute result = null;
        MonitoredEntry entry = _nameToAttrMap.get(name);
        if (entry != null) {
            try {
                result = new Attribute(name, entry.getValue());
                logger.debug("getAttribute(): " + name
                        + " => "
                        + result.getValue());
            } catch (Exception e) {
                logger.warn("Failed to getAttribute() on:  " + name, e);
            }
        } else {
            logger.warn("Did not find queried attribute with name " + name);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Object invoke(String actionName, Object params[], String signature[])
            throws MBeanException, ReflectionException {
        throw new IllegalArgumentException("invoke(...) is not supported on this mbean");
    }

    /**
     * {@inheritDoc}
     */
    public void setAttribute(Attribute attribute)
            throws AttributeNotFoundException, InvalidAttributeValueException,
            MBeanException, ReflectionException {
        throw new IllegalArgumentException("setAttribute() is not supported on this mbean");
    }

    /**
     * {@inheritDoc}
     */
    public AttributeList setAttributes(AttributeList list) {
        throw new IllegalArgumentException("setAttributes() is not supported on this mbean");
    }

    public ObjectName getObjectName() {
        return _objectName;
    }

    private Object getResourceObject() {
        return _resourceToMonitor;
    }

    private ObjectName createObjectName(MonitorRegistry.Namespace namespace) {
        try {
            StringBuilder nameBuf = new StringBuilder(MonitorRegistry.DOMAIN_NAME);
            nameBuf.append(":type=").append(namespace.name()).
                    append(",name=").
                    append(quoteValue(getResourceObject().getClass().getSimpleName()));
            if (_displayName != null) {
                nameBuf.append(",instance=").append(quoteValue(_displayName));
            }
            return new ObjectName(nameBuf.toString());
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static String quoteValue(String value) {
        if (value != null && value.matches(".*[:\"\\*\\?\\\\\n].*")) {
            value = ObjectName.quote(value);
        }
        return value;
    }

    private static SimpleType toSimpleType(Class<?> cls) {
        if (cls == String.class) {
            return SimpleType.STRING;
        }
        if (cls == Integer.class || cls == Integer.TYPE) {
            return SimpleType.INTEGER;
        }
        if (cls == Boolean.class || cls == Boolean.TYPE) {
            return SimpleType.BOOLEAN;
        }
        if (cls == Double.class || cls == Double.TYPE) {
            return SimpleType.DOUBLE;
        }
        if (cls == Float.class || cls == Float.TYPE) {
            return SimpleType.FLOAT;
        }
        if (cls == Long.class || cls == Long.TYPE) {
            return SimpleType.LONG;
        }
        if (cls == Byte.class || cls == Byte.TYPE) {
            return SimpleType.BYTE;
        }
        if (cls == Short.class || cls == Short.TYPE) {
            return SimpleType.SHORT;
        }
        if (cls == Character.class || cls == Character.TYPE) {
            return SimpleType.CHARACTER;
        }
        if (cls == Date.class) {
            return SimpleType.DATE;
        }
        return SimpleType.STRING;
    }

    private class MonitoredEntry {
        private OpenMBeanAttributeInfoSupport _info;
        private CompositeType _openType;
        private final Field _field;
        private final Method _method;
        private final Monitor _monitor;
        private SimpleType _valType;

        MonitoredEntry(final Monitor monitor, final Method method) throws OpenDataException {

            _field = null;
            _method = method;
            _monitor = monitor;
            setupTypeInfo();
        }

        MonitoredEntry(final Monitor monitor, final Field field) throws OpenDataException {

            _field = field;
            _method = null;
            _monitor = monitor;

            if (!_field.isAccessible()) {
                _field.setAccessible(true);
            }

            setupTypeInfo();
        }

        private void setupTypeInfo() throws OpenDataException {
            _valType = toSimpleType(_field != null ? _field.getType() :
                    _method.getReturnType());

            _openType = new CompositeType(
                    TYPE_NAME,
                    TYPE_DESC,
                    ITEM_NAMES,
                    ITEM_NAMES,
                    new OpenType[]{
                            SimpleType.STRING,
                            SimpleType.STRING,
                            SimpleType.STRING,
                            SimpleType.STRING,
                            SimpleType.STRING,
                            SimpleType.STRING,
                            _valType
                    }
            );

            _info = new OpenMBeanAttributeInfoSupport(
                    _monitor.dataSourceName(),
                    _monitor.description().trim().equals("") ? _monitor.dataSourceName() :
                            _monitor.description(),
                    _openType,
                    true,  //readable?
                    false, //writable?
                    false  //isIs?
            );
        }

        public Object getValue() throws Exception {
            Object val = _field != null ? _field.get(getResourceObject()) :
                    _method.invoke(getResourceObject(), new Object[]{});

            if (val != null && _valType == SimpleType.STRING &&
                    !(val instanceof String)) {
                val = val.toString();
            }

            CompositeDataSupport data =
                    new CompositeDataSupport(_openType,
                            ITEM_NAMES,
                            new Object[]{
                                    _monitor.dataSourceName(),
                                    _monitor.type().name(),
                                    _monitor.description(),
                                    _monitor.min(),
                                    _monitor.max(),
                                    _monitor.expectedValue(),
                                    val
                            }
                    );
            return data;
        }

        public MBeanAttributeInfo getInfo() {
            return _info;
        }
    }
}
