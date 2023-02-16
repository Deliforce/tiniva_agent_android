package com.app.tiniva.RawHeaders.TaskInfo;

import com.google.gson.annotations.SerializedName;

public class DeleteNotHeader {

    @SerializedName("data")

    private DeleteNote filter;

    public DeleteNote getFilter() {
        return filter;
    }

    public void setFilter(DeleteNote filter) {
        this.filter = filter;
    }
}
