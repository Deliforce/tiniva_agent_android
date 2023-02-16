package com.app.tiniva.RawHeaders.Analytics;

import com.google.gson.annotations.SerializedName;

public class HoursFilter {

    @SerializedName("filter")
    private int status;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }


}
