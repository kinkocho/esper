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
package com.espertech.esper.regression.enummethod;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_ST0;
import com.espertech.esper.supportregression.bean.SupportBean_ST0_Container;
import com.espertech.esper.supportregression.bean.SupportCollection;
import com.espertech.esper.supportregression.bean.lambda.LambdaAssertionUtil;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.util.EventRepresentationChoice;
import junit.framework.TestCase;

import java.util.*;

public class TestEnumExceptIntersectUnion extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp() {

        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean_ST0_Container", SupportBean_ST0_Container.class);
        config.addEventType("SupportBean_ST0", SupportBean_ST0.class);
        config.addEventType("SupportBean", SupportBean.class);
        config.addEventType("SupportCollection", SupportCollection.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testStringArrayIntersection() throws Exception {
        String epl = "create objectarray schema Event(meta1 string[], meta2 string[]);\n" +
                "@Name('Out') select * from Event(meta1.intersect(meta2).countOf() > 0);\n";
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
        epService.getEPAdministrator().getStatement("Out").addListener(listener);

        sendAndAssert("a,b", "a,b", true);
        sendAndAssert("c,d", "a,b", false);
        sendAndAssert("c,d", "a,d", true);
        sendAndAssert("a,d,a,a", "b,c", false);
        sendAndAssert("a,d,a,a", "b,d", true);
    }

    public void testSetLogicWithContained() {
        String epl = "select " +
                "contained.except(containedTwo) as val0," +
                "contained.intersect(containedTwo) as val1, " +
                "contained.union(containedTwo) as val2 " +
                " from SupportBean_ST0_Container";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmt.getEventType(), "val0".split(","), new Class[]{Collection.class});

        List<SupportBean_ST0> first = SupportBean_ST0_Container.make2ValueList("E1,1", "E2,10", "E3,1", "E4,10", "E5,11");
        List<SupportBean_ST0> second = SupportBean_ST0_Container.make2ValueList("E1,1", "E3,1", "E4,10");
        epService.getEPRuntime().sendEvent(new SupportBean_ST0_Container(first, second));
        LambdaAssertionUtil.assertST0Id(listener, "val0", "E2,E5");
        LambdaAssertionUtil.assertST0Id(listener, "val1", "E1,E3,E4");
        LambdaAssertionUtil.assertST0Id(listener, "val2", "E1,E2,E3,E4,E5,E1,E3,E4");
        listener.reset();
    }

    public void testSetLogicWithEvents() {

        String epl =
                "expression last10A {" +
                " (select * from SupportBean_ST0(key0 like 'A%')#length(2)) " +
                "}" +
                "expression last10NonZero {" +
                " (select * from SupportBean_ST0(p00 > 0)#length(2)) " +
                "}" +
                "select " +
                "last10A().except(last10NonZero()) as val0," +
                "last10A().intersect(last10NonZero()) as val1, " +
                "last10A().union(last10NonZero()) as val2 " +
                "from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmt.getEventType(), "val0".split(","), new Class[]{Collection.class});

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E1", "A1", 10));    // in both
        epService.getEPRuntime().sendEvent(new SupportBean());
        LambdaAssertionUtil.assertST0Id(listener, "val0", "");
        LambdaAssertionUtil.assertST0Id(listener, "val1", "E1");
        LambdaAssertionUtil.assertST0Id(listener, "val2", "E1,E1");
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E2", "A1", 0));
        epService.getEPRuntime().sendEvent(new SupportBean());
        LambdaAssertionUtil.assertST0Id(listener, "val0", "E2");
        LambdaAssertionUtil.assertST0Id(listener, "val1", "E1");
        LambdaAssertionUtil.assertST0Id(listener, "val2", "E1,E2,E1");
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E3", "B1", 0));
        epService.getEPRuntime().sendEvent(new SupportBean());
        LambdaAssertionUtil.assertST0Id(listener, "val0", "E2");
        LambdaAssertionUtil.assertST0Id(listener, "val1", "E1");
        LambdaAssertionUtil.assertST0Id(listener, "val2", "E1,E2,E1");
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E4", "A2", -1));    
        epService.getEPRuntime().sendEvent(new SupportBean());
        LambdaAssertionUtil.assertST0Id(listener, "val0", "E2,E4");
        LambdaAssertionUtil.assertST0Id(listener, "val1", "");
        LambdaAssertionUtil.assertST0Id(listener, "val2", "E2,E4,E1");
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E5", "A3", -2));
        epService.getEPRuntime().sendEvent(new SupportBean());
        LambdaAssertionUtil.assertST0Id(listener, "val0", "E4,E5");
        LambdaAssertionUtil.assertST0Id(listener, "val1", "");
        LambdaAssertionUtil.assertST0Id(listener, "val2", "E4,E5,E1");
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E6", "A6", 11));    // in both
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E7", "A7", 12));    // in both
        epService.getEPRuntime().sendEvent(new SupportBean());
        LambdaAssertionUtil.assertST0Id(listener, "val0", "");
        LambdaAssertionUtil.assertST0Id(listener, "val1", "E6,E7");
        LambdaAssertionUtil.assertST0Id(listener, "val2", "E6,E7,E6,E7");
        listener.reset();
    }

    public void testSetLogicWithScalar() {
        String epl = "select " +
                "strvals.except(strvalstwo) as val0," +
                "strvals.intersect(strvalstwo) as val1, " +
                "strvals.union(strvalstwo) as val2 " +
                " from SupportCollection as bean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmt.getEventType(), "val0".split(","), new Class[]{Collection.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1,E2", "E3,E4"));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", "E1", "E2");
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val1");
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val2", "E1", "E2", "E3", "E4");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(null, "E3,E4"));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", (Object[]) null);
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val1", (Object[]) null);
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val2", (Object[]) null);
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("", "E3,E4"));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0");
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val1");
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val2", "E3", "E4");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1,E3,E5", "E3,E4"));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", "E1", "E5");
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val1", "E3");
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val2", "E1", "E3", "E5", "E3", "E4");
        listener.reset();
    }

    public void testInvalid() {
        String epl;

        epl = "select contained.union(true) from SupportBean_ST0_Container";
        tryInvalid(epl, "Error starting statement: Failed to validate select-clause expression 'contained.union(true)': Enumeration method 'union' requires an expression yielding an event-collection as input paramater [select contained.union(true) from SupportBean_ST0_Container]");

        epl = "select contained.union(prevwindow(s1)) from SupportBean_ST0_Container#lastevent, SupportBean#keepall s1";
        tryInvalid(epl, "Error starting statement: Failed to validate select-clause expression 'contained.union(prevwindow(s1))': Enumeration method 'union' expects event type 'SupportBean_ST0' but receives event type 'SupportBean' [select contained.union(prevwindow(s1)) from SupportBean_ST0_Container#lastevent, SupportBean#keepall s1]");
    }

    public void testUnionWhere() {

        String epl = "expression one {" +
                "  x => x.contained.where(y => p00 = 10)" +
                "} " +
                "" +
                "expression two {" +
                "  x => x.contained.where(y => p00 = 11)" +
                "} " +
                "" +
                "select one(bean).union(two(bean)) as val0 from SupportBean_ST0_Container as bean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmt.getEventType(), "val0".split(","), new Class[]{Collection.class});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1", "E2,10", "E3,1", "E4,10", "E5,11"));
        LambdaAssertionUtil.assertST0Id(listener, "val0", "E2,E4,E5");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,10", "E2,1", "E3,1"));
        LambdaAssertionUtil.assertST0Id(listener, "val0", "E1");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1", "E2,1", "E3,10", "E4,11"));
        LambdaAssertionUtil.assertST0Id(listener, "val0", "E3,E4");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value((String[])null));
        LambdaAssertionUtil.assertST0Id(listener, "val0", null);
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value());
        LambdaAssertionUtil.assertST0Id(listener, "val0", "");
        listener.reset();
    }

    public void testInheritance() {
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            if (rep.isMapEvent() || rep.isObjectArrayEvent()) {
                runAssertionInheritance(rep);
            }
        }
    }

    public void runAssertionInheritance(EventRepresentationChoice eventRepresentationEnum) {

        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema BaseEvent as (b1 string)");
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema SubEvent as (s1 string) inherits BaseEvent");
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema OuterEvent as (bases BaseEvent[], subs SubEvent[])");
        EPStatement stmt = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " select bases.union(subs) as val from OuterEvent");
        stmt.addListener(listener);

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[] {new Object[][] {{"b10"}}, new Object[][] {{"b10", "s10"}}}, "OuterEvent");
        }
        else {
            Map<String, Object> baseEvent = makeMap("b1", "b10");
            Map<String, Object> subEvent = makeMap("s1", "s10");
            Map<String, Object> outerEvent = makeMap("bases", new Map[] {baseEvent}, "subs", new Map[] {subEvent});
            epService.getEPRuntime().sendEvent(outerEvent, "OuterEvent");
        }

        Collection result = (Collection) listener.assertOneGetNewAndReset().get("val");
        assertEquals(2, result.size());

        epService.initialize();
    }

    private void tryInvalid(String epl, String message) {
        try
        {
            epService.getEPAdministrator().createEPL(epl);
            fail();
        }
        catch (EPStatementException ex) {
            assertEquals(message, ex.getMessage());
        }
    }

    private Map<String, Object> makeMap(String key, Object value) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put(key, value);
        return map;
    }

    private Map<String, Object> makeMap(String key, Object value, String key2, Object value2) {
        Map<String, Object> map = makeMap(key, value);
        map.put(key2, value2);
        return map;
    }

    private void sendAndAssert(String metaOne, String metaTwo, boolean expected) {
        epService.getEPRuntime().sendEvent(new Object[] {metaOne.split(","), metaTwo.split(",")}, "Event");
        assertEquals(expected, listener.getIsInvokedAndReset());
    }
}
