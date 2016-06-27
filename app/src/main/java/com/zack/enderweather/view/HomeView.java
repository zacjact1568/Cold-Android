package com.zack.enderweather.view;

import com.zack.enderweather.adapter.WeatherPagerAdapter;

public interface HomeView {

    void showInitialView(WeatherPagerAdapter weatherPagerAdapter);

    void showToast(String message);

    void onDetectNetworkNotAvailable();

    void onSwitchPage(int position);
}
