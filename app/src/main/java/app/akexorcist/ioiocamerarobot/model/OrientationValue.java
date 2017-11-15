package app.akexorcist.ioiocamerarobot.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by OldMan on 07.11.2016.
 */

public class OrientationValue {
    @SerializedName("value")
    private float[] value;

    public OrientationValue(float[] value) {
        this.value = value;
    }

    public float[] getValue() {
        return value;
    }
}
