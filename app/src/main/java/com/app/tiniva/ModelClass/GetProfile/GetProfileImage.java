package com.app.tiniva.ModelClass.GetProfile;

import com.google.gson.annotations.SerializedName;

public class GetProfileImage {

    @SerializedName("imageUrl")
    private String driverProfileImg;

    public String getDriverProfileImg() {
        return driverProfileImg;
    }
}
