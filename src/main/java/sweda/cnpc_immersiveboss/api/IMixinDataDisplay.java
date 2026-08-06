package sweda.cnpc_immersiveboss.api;

public interface IMixinDataDisplay {

    String getCustomBossBar();

    void setCustomBossBar(String url);

    int getCustomBossColor();

    void setCustomBossColor(int color);

    int getCustomBossXShift();

    void setCustomBossXShift(int xShift);

    double getCustomBossXScale();

    double getCustomBossYScale();

    void setCustomBossXScale(double xScale);

    void setCustomBossYScale(double yScale);

    int getCustomBossWidth();

    void setCustomBossWidth(int width);

    int getCustomBossHeight();

    void setCustomBossHeight(int height);
}
