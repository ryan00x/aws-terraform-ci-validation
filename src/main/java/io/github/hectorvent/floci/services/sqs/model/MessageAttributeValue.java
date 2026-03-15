package io.github.hectorvent.floci.services.sqs.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageAttributeValue {

    private String stringValue;
    private String dataType;

    public MessageAttributeValue() {}

    public MessageAttributeValue(String stringValue, String dataType) {
        this.stringValue = stringValue;
        this.dataType = dataType;
    }

    public String getStringValue() { return stringValue; }
    public void setStringValue(String stringValue) { this.stringValue = stringValue; }

    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
}
