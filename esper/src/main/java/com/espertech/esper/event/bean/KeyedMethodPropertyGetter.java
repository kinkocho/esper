/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.event.bean;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventPropertyGetterAndIndexed;
import com.espertech.esper.event.EventPropertyGetterAndMapped;
import com.espertech.esper.event.vaevent.PropertyUtility;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Getter for a key property identified by a given key value, using vanilla reflection.
 */
public class KeyedMethodPropertyGetter extends BaseNativePropertyGetter implements BeanEventPropertyGetter, EventPropertyGetterAndMapped, EventPropertyGetterAndIndexed {
    private final Method method;
    private final Object key;

    /**
     * Constructor.
     *
     * @param method              is the method to use to retrieve a value from the object.
     * @param key                 is the key to supply as parameter to the mapped property getter
     * @param eventAdapterService factory for event beans and event types
     */
    public KeyedMethodPropertyGetter(Method method, Object key, EventAdapterService eventAdapterService) {
        super(eventAdapterService, method.getReturnType(), null);
        this.key = key;
        this.method = method;
    }

    public Object get(EventBean eventBean, int index) throws PropertyAccessException {
        return getBeanPropInternal(eventBean.getUnderlying(), index);
    }

    public Object get(EventBean eventBean, String mapKey) throws PropertyAccessException {
        return getBeanPropInternal(eventBean.getUnderlying(), mapKey);
    }

    public Object getBeanProp(Object object) throws PropertyAccessException {
        return getBeanPropInternal(object, key);
    }

    private Object getBeanPropInternal(Object object, Object key) throws PropertyAccessException {
        try {
            return method.invoke(object, key);
        } catch (ClassCastException e) {
            throw PropertyUtility.getMismatchException(method, object, e);
        } catch (InvocationTargetException e) {
            throw PropertyUtility.getInvocationTargetException(method, e);
        } catch (IllegalAccessException e) {
            throw PropertyUtility.getIllegalAccessException(method, e);
        } catch (IllegalArgumentException e) {
            throw PropertyUtility.getIllegalArgumentException(method, e);
        }
    }

    public boolean isBeanExistsProperty(Object object) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public final Object get(EventBean obj) throws PropertyAccessException {
        Object underlying = obj.getUnderlying();
        return getBeanProp(underlying);
    }

    public String toString() {
        return "KeyedMethodPropertyGetter " +
                " method=" + method.toString() +
                " key=" + key;
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }
}
