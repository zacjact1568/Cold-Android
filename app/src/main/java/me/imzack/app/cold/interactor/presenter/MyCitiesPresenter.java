package me.imzack.app.cold.interactor.presenter;

import me.imzack.app.cold.event.WeatherUpdateStatusChangedEvent;
import me.imzack.app.cold.interactor.adapter.CityAdapter;
import me.imzack.app.cold.model.database.DatabaseDispatcher;
import me.imzack.app.cold.event.CityAddedEvent;
import me.imzack.app.cold.event.CityClickedEvent;
import me.imzack.app.cold.event.CityDeletedEvent;
import me.imzack.app.cold.model.ram.DataManager;
import me.imzack.app.cold.util.Util;
import me.imzack.app.cold.domain.view.MyCitiesView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class MyCitiesPresenter implements Presenter<MyCitiesView> {

    private static final String LOG_TAG = "MyCitiesPresenter";

    private MyCitiesView myCitiesView;
    private DataManager dataManager;
    private DatabaseDispatcher databaseDispatcher;
    private CityAdapter cityAdapter;

    public MyCitiesPresenter(MyCitiesView myCitiesView) {
        attachView(myCitiesView);
        dataManager = DataManager.getInstance();
        databaseDispatcher = DatabaseDispatcher.getInstance();
        cityAdapter = new CityAdapter(dataManager.getWeatherList());
    }

    @Override
    public void attachView(MyCitiesView view) {
        myCitiesView = view;
        EventBus.getDefault().register(this);
    }

    @Override
    public void detachView() {
        myCitiesView = null;
        EventBus.getDefault().unregister(this);
    }

    public void setInitialView() {
        cityAdapter.setOnCityItemClickListener(new CityAdapter.OnCityItemClickListener() {
            @Override
            public void onCityItemClick(int position) {
                myCitiesView.onBack();
                EventBus.getDefault().post(new CityClickedEvent(position));
            }
        });
        cityAdapter.setOnUpdateButtonClickListener(new CityAdapter.OnUpdateButtonClickListener() {
            @Override
            public void onUpdateButtonClick(int position) {
                if (dataManager.getWeatherDataUpdateStatus(position)) {
                    //说明现在正在更新，不响应请求
                    return;
                }
                updateWeather(position);
            }
        });
        cityAdapter.setOnDeleteButtonClickListener(new CityAdapter.OnDeleteButtonClickListener() {
            @Override
            public void onDeleteButtonClick(int position) {
                myCitiesView.showCityDeletionAlertDialog(dataManager.getCityName(position), position);
            }
        });
        myCitiesView.showInitialView(cityAdapter);
    }

    public void notifyCityDeleted(int position) {
        String cityId = dataManager.getCityId(position);
        dataManager.removeFromWeatherList(position);
        cityAdapter.notifyItemRemoved(position);

        //更新天气的ViewPager
        EventBus.getDefault().post(new CityDeletedEvent());

        databaseDispatcher.deleteWeather(cityId);
    }

    private void updateWeather(int position) {
        if (Util.isNetworkAvailable()) {
            //开始更新数据
            dataManager.getWeatherDataFromInternet(dataManager.getCityId(position));
            //刷新适配器（显示出正在更新的状态）
            cityAdapter.notifyItemChanged(position);
        } else {
            //显示网络不可用的SnackBar
            myCitiesView.onDetectNetworkNotAvailable();
        }
    }

    @Subscribe
    public void onCityAdded(CityAddedEvent event) {
        cityAdapter.notifyItemInserted(dataManager.getRecentlyAddedLocation());
    }

    @Subscribe
    public void onWeatherUpdateStatusChanged(WeatherUpdateStatusChangedEvent event) {
        //刷新适配器：
        //1. 正在更新，表现为添加正在更新的状态（旋转箭头）
        //2. 成功更新，表现为刷新数据且取消正在更新的状态
        //3. 更新失败，表现为仅取消正在更新的状态
        cityAdapter.notifyItemChanged(event.position);
    }
}