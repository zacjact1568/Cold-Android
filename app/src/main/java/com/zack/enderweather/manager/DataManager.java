package com.zack.enderweather.manager;

import com.zack.enderweather.bean.HeWeather;
import com.zack.enderweather.bean.Weather;
import com.zack.enderweather.database.EnderWeatherDB;
import com.zack.enderweather.event.WeatherUpdatedEvent;
import com.zack.enderweather.network.NetworkHelper;
import com.zack.enderweather.util.LogUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

public class DataManager {

    private static final String LOG_TAG = "DataManager";

    private EnderWeatherDB enderWeatherDB;
    private List<Weather> weatherList;

    private static DataManager ourInstance = new DataManager();

    private DataManager() {
        enderWeatherDB = EnderWeatherDB.getInstance();
        weatherList = new ArrayList<>();
    }

    public static DataManager getInstance() {
        return ourInstance;
    }

    public List<Weather> getWeatherList() {
        return weatherList;
    }

    public Weather getWeather(int location) {
        return weatherList.get(location);
    }

    public Weather getWeather(String cityId) {
        return weatherList.get(getLocationInWeatherList(cityId));
    }

    public void addToWeatherList(String cityId, String cityName) {
        weatherList.add(new Weather(cityId, cityName));
    }

    public int getWeatherCount() {
        return weatherList.size();
    }

    /** 获取最近添加的城市ID */
    public String getRecentlyAddedCityId() {
        return getWeather(getWeatherCount() - 1).getBasicInfo().getCityId();
    }

    /** 发起网络访问，获取天气数据 */
    public void getWeatherDataFromInternet(String cityId) {
        new NetworkHelper().getHeWeatherDataAsync(cityId, new NetworkHelper.HeWeatherDataCallback() {
            @Override
            public void onSuccess(HeWeather heWeather) {
                parseHeWeatherData(heWeather);
                EventBus.getDefault().post(new WeatherUpdatedEvent(heWeather.getHeWeatherAPIList().get(0).getBasic().getId()));
            }

            @Override
            public void onFailure(String msg) {
                LogUtil.e(LOG_TAG, msg);
            }
        });
    }

    public int getLocationInWeatherList(String cityId) {
        for (int i = 0; i < getWeatherCount(); i++) {
            if (getWeather(i).getBasicInfo().getCityId().equals(cityId)) {
                return i;
            }
        }
        throw new RuntimeException("No Weather object matching city id " + cityId);
    }

    /** 将和风天气bean中的内容解析到已存在本地的天气bean中 */
    public void parseHeWeatherData(HeWeather heWeather) {
        HeWeather.HeWeatherAPI api = heWeather.getHeWeatherAPIList().get(0);

        HeWeather.HeWeatherAPI.Basic basic = api.getBasic();
        HeWeather.HeWeatherAPI.Now now = api.getNow();
        HeWeather.HeWeatherAPI.Aqi.City cityAqi = api.getAqi().getCity();
        HeWeather.HeWeatherAPI.Suggestion suggestion = api.getSuggestion();

        //获取本地的天气bean
        Weather weather = getWeather(basic.getId());

        weather.getBasicInfo().setExtraValues(
                basic.getCity(),
                basic.getUpdate().getLoc()
        );

        weather.getCurrentInfo().setExtraValues(
                now.getCond().getTxt(),
                now.getTmp(),
                now.getFl(),
                now.getHum(),
                now.getPcpn(),
                now.getPres(),
                now.getVis(),
                now.getWind().getSpd(),
                now.getWind().getSc(),
                now.getWind().getDeg(),
                now.getWind().getDir()
        );

        int startIndex = Weather.HOURLY_FORECAST_LENGTH - api.getHourlyForecastList().size();
        for (int i = 0; i < Weather.HOURLY_FORECAST_LENGTH; i++) {
            if (i < startIndex) {
                weather.getHourlyForecastList().get(i).clearExtraValues();
            } else {
                HeWeather.HeWeatherAPI.HourlyForecast hourlyForecast = api.getHourlyForecastList().get(i - startIndex);
                weather.getHourlyForecastList().get(i).setExtraValues(
                        hourlyForecast.getDate(),
                        hourlyForecast.getTmp(),
                        hourlyForecast.getWind().getSpd(),
                        hourlyForecast.getWind().getSc(),
                        hourlyForecast.getWind().getDeg(),
                        hourlyForecast.getWind().getDir(),
                        hourlyForecast.getPop(),
                        hourlyForecast.getHum(),
                        hourlyForecast.getPres()
                );
            }
        }

        for (int i = 0; i < Weather.DAILY_FORECAST_LENGTH; i++) {
            HeWeather.HeWeatherAPI.DailyForecast dailyForecast = api.getDailyForecastList().get(i);
            weather.getDailyForecastList().get(i).setExtraValues(
                    dailyForecast.getDate(),
                    dailyForecast.getAstro().getSr(),
                    dailyForecast.getAstro().getSs(),
                    dailyForecast.getTmp().getMax(),
                    dailyForecast.getTmp().getMin(),
                    dailyForecast.getWind().getSpd(),
                    dailyForecast.getWind().getSc(),
                    dailyForecast.getWind().getDeg(),
                    dailyForecast.getWind().getDir(),
                    dailyForecast.getCond().getTxtD(),
                    dailyForecast.getCond().getTxtN(),
                    dailyForecast.getPcpn(),
                    dailyForecast.getPop(),
                    dailyForecast.getHum(),
                    dailyForecast.getPres(),
                    dailyForecast.getVis()
            );
        }

        weather.getAirQuality().setExtraValues(
                cityAqi.getAqi(),
                cityAqi.getPm10(),
                cityAqi.getPm25()
        );

        weather.getLifeSuggestion().setExtraValues(
                suggestion.getComf().getBrf(),
                suggestion.getDrsg().getBrf(),
                suggestion.getUv().getBrf(),
                suggestion.getCw().getBrf(),
                suggestion.getTrav().getBrf(),
                suggestion.getFlu().getBrf(),
                suggestion.getSport().getBrf()
        );
    }
}
