package com.app.tiniva.RawHeaders.TaskInfo;

import com.google.gson.annotations.SerializedName;

public class FilterMain {

    @SerializedName("filter")
    private Filter filter;
    @SerializedName("timezone")
    private String timezone;

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }
}
