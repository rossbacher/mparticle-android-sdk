package com.mparticle.internal;


import android.content.Intent;

import com.mparticle.MPEvent;
import com.mparticle.MPProduct;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.internal.embedded.EmbeddedProvider;
import com.mparticle.internal.embedded.Projection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.Map;

public class ReportingMessage {
    private final int moduleId;
    private String messageType;
    private final double timestamp;
    private Map<String, String> attributes;
    private String eventName = null;
    private String eventType = null;
    private LinkedList<ProjectionReport> projectionReports;
    private String screenName;
    private boolean devMode;

    public ReportingMessage(EmbeddedProvider provider, String messageType, double timestamp, Map<String, String> attributes) {
        this.moduleId = provider.getModuleId();
        this.messageType = messageType;
        this.timestamp = timestamp;
        this.attributes = attributes;
    }

    public static ReportingMessage fromPushMessage(EmbeddedProvider provider, Intent intent) {
        return new ReportingMessage(provider, Constants.MessageType.PUSH_RECEIVED, System.currentTimeMillis(), null);
    }

    public static ReportingMessage fromPushRegistrationMessage(EmbeddedProvider provider, Intent intent) {
        return new ReportingMessage(provider, Constants.MessageType.PUSH_REGISTRATION, System.currentTimeMillis(), null);
    }

    public static ReportingMessage fromTransaction(EmbeddedProvider provider, MPProduct product) {
        return new ReportingMessage(provider, Constants.MessageType.EVENT, System.currentTimeMillis(), product);
    }

    public ReportingMessage setEventName(String eventName) {
        this.eventName = eventName;
        return this;
    }

    public ReportingMessage setAttributes(Map<String, String> eventAttributes) {
        attributes = eventAttributes;
        return this;
    }

    public String getEventName() {
        return eventName;
    }

    public String getEventTypeString() {
        return eventType;
    }

    public void addProjectionReport(ProjectionReport report) {
        if (projectionReports == null) {
            projectionReports = new LinkedList<ProjectionReport>();
        }
        projectionReports.add(report);
    }

    public static ReportingMessage logoutMessage(EmbeddedProvider provider) {
        return new ReportingMessage(provider,
                Constants.MessageType.PROFILE,
                System.currentTimeMillis(),
                null);
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public static ReportingMessage fromEvent(EmbeddedProvider provider, MPEvent event) {
        ReportingMessage message = new ReportingMessage(provider, Constants.MessageType.EVENT, System.currentTimeMillis(), event.getInfo());
        message.eventType = event.getEventType().name();
        message.eventName = event.getEventName();
        return message;
    }

    public static ReportingMessage fromEvent(EmbeddedProvider provider, CommerceEvent commerceEvent) {
        ReportingMessage message = new ReportingMessage(provider, Constants.MessageType.COMMERCE_EVENT, System.currentTimeMillis(), commerceEvent.getCustomAttributes());
        message.eventType = CommerceEventUtil.getEventTypeString(commerceEvent);
        return message;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("mid", moduleId);
        jsonObject.put("dt", messageType);
        jsonObject.put("ct", timestamp);
        if (projectionReports != null) {
            JSONArray reports = new JSONArray();
            for (int i = 0; i < projectionReports.size(); i++) {
                JSONObject report = new JSONObject();
                report.put("pid", projectionReports.get(i).projectionId);
                report.put("dt", projectionReports.get(i).messageType);
                report.put("name", projectionReports.get(i).eventName);
                report.put("et", projectionReports.get(i).eventType);
                reports.put(report);
            }
            if (reports.length() > 0) {
                jsonObject.put("proj", reports);
            }
        }
        if (devMode && attributes != null && attributes.size() > 0) {
            JSONObject attributeJson = new JSONObject();
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                attributeJson.put(entry.getKey(), entry.getValue());
            }
            jsonObject.put("attrs", attributeJson);
        }
        if (!MPUtility.isEmpty(eventType)) {
            jsonObject.put("et", eventType);
        }
        if (messageType.equals(Constants.MessageType.EVENT)) {
            if (!MPUtility.isEmpty(eventName)) {
                jsonObject.put("n", eventName);
            }
        } else if (messageType.equals(Constants.MessageType.SCREEN_VIEW)) {
            if (!MPUtility.isEmpty(screenName)) {
                jsonObject.put("n", screenName);
            }
        } else if (messageType.equals(Constants.MessageType.PUSH_REGISTRATION)) {
            jsonObject.put("pr", true);
        }

        return jsonObject;
    }

    public double getTimestamp() {
        return timestamp;
    }

    public int getModuleId() {
        return moduleId;
    }

    public static class ProjectionReport {
        private final int projectionId;
        private final String messageType;
        private final String eventName;
        private final String eventType;

        public ProjectionReport(int projectionId, String messageType, String eventName, String eventType) {
            this.projectionId = projectionId;
            this.messageType = messageType;
            this.eventName = eventName;
            this.eventType = eventType;
        }

        public static ProjectionReport fromEvent(int projectionId, MPEvent event) {
            return new ProjectionReport(
                    projectionId,
                    Constants.MessageType.EVENT,
                    event.getEventName(),
                    event.getEventType().name());
        }

        public static ProjectionReport fromEvent(int projectionId, CommerceEvent event) {
            return new ProjectionReport(
                    projectionId,
                    Constants.MessageType.EVENT,
                    event.getEventName(),
                    CommerceEventUtil.getEventTypeString(event));
        }

        public static ProjectionReport fromProjectionResult(Projection.ProjectionResult result) {
            if (result.getMPEvent() != null) {
                return fromEvent(result.getProjectionId(), result.getMPEvent());
            } else {
                return fromEvent(result.getProjectionId(), result.getCommerceEvent());
            }
        }
    }
}
