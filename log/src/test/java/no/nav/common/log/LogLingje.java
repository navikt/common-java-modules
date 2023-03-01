package no.nav.common.log;

import com.google.gson.annotations.SerializedName;

public class LogLingje {

    @SerializedName("@timestamp")
    public String timestamp;
    @SerializedName("@version")
    public String version;
    public String message;
    public String logger_name;
    public String thread_name;
    public String level;
    public int level_value;
}
