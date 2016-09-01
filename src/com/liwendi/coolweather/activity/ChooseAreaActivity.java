package com.liwendi.coolweather.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.liwendi.coolweather.R;
import com.liwendi.coolweather.db.CoolWeatherDB;
import com.liwendi.coolweather.model.City;
import com.liwendi.coolweather.model.County;
import com.liwendi.coolweather.model.Province;
import com.liwendi.coolweather.util.HttpCallbackListener;
import com.liwendi.coolweather.util.HttpUtil;
import com.liwendi.coolweather.util.Utility;

public class ChooseAreaActivity extends Activity implements HttpCallbackListener{
	public static final int LEVEL_PROVINCE = 0;
	public static final int LEVEL_CITY = 1;
	public static final int LEVEL_COUNTY = 2;
	private ProgressDialog progressDialog;
	private TextView titleText;
	private ListView listView;
	private ArrayAdapter<String> adapter;
	private CoolWeatherDB coolWeatherDB;
	private List<String> datalist = new ArrayList<String>();
	/**
	 * 省列表
	 */
	private List<Province> provinceList;
	/**
	 * 市列表
	 */
	private List<City> cityList;
	/**
	 * 县列表
	 */
	private List<County> countyList;
	/**
	 * 选中省份
	 */
	private Province selectedProvince;
	/**
	 * 选中城市
	 */
	private City selectedCity;
	/**
	 * 选中县
	 */
	private County selectedCounty;
	/**
	 * 当前选中的级别
	 */
	private int currentLevel;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.choose_area);
		listView = (ListView) findViewById(R.id.list_view);
		titleText = (TextView) findViewById(R.id.title_text);
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,datalist);
		listView.setAdapter(adapter);
		coolWeatherDB = CoolWeatherDB.getInstance(this);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if(currentLevel == LEVEL_PROVINCE){
					selectedProvince = provinceList.get(position);
					queryCities();
				}else if(currentLevel == LEVEL_CITY){
					selectedCity = cityList.get(position);
					queryCounties();
				}
			}
		});
		queryProvinces();//加载省级数据
	}
	/**
	 * 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询
	 */
	private void queryProvinces(){
		provinceList = coolWeatherDB.loadProvince();
		if(provinceList.size() > 0){
			datalist.clear();
			for(Province province : provinceList){
				datalist.add(province.getProvinceName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText("中国");
			currentLevel = LEVEL_PROVINCE;
		}else{
			queryFromServer(null,"province");
		}
	}
	/**
	 * 查询选中省内所有的市，优先从数据库查询，如果没有查询到，再去服务器上查询
	 */
	private void queryCities(){
		cityList = coolWeatherDB.loadCities(selectedProvince.getId());
		if(cityList.size() > 0){
			datalist.clear();
			for(City city : cityList){
				datalist.add(city.getCityName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedProvince.getProvinceName());
			currentLevel = LEVEL_CITY;
		}else{
			queryFromServer(selectedProvince.getProvinceCode(), "city");
		}
	}
	/**
	 * 查询选中市内所有的县，优先从数据库查询，如果没有查询到，再去服务器上查询
	 */
	private void queryCounties(){
		countyList = coolWeatherDB.loadCounties(selectedCity.getId());
		if(countyList.size() > 0){
			datalist.clear();
			for(County county : countyList){
				datalist.add(county.getCountyName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedCity.getCityName());
			currentLevel = LEVEL_COUNTY;
		}else{
			queryFromServer(selectedCity.getCityCode(), "county");
		}
	}
	/**
	 * 根据传入的代号和类型从服务器上查询省市县数据
	 */
	private void queryFromServer(final String code,final String flag){
		String address;
		if(!TextUtils.isEmpty(code)){
			address = "http://www.weather.com.cn/data/list3/city"+code+".xml";
		}else{
			address = "http://www.weather.com.cn/data/list3/city.xml";
		}
		showProgressDialog();
//		Log.e("test", "请求链接="+address);
		HttpUtil.sendHttpRequest(address,this,flag);
	}
	/**
	 * 显示进度对话框
	 */
	private void showProgressDialog(){
		if(progressDialog == null){
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("正在加载...");
			progressDialog.setCancelable(false);
		}
		progressDialog.show();
	}
	/**
	 * 关闭进度对话框
	 */
	private void colseProgressDialog(){
		if(progressDialog != null){
			progressDialog.dismiss();
		}
	}
	@Override
	public void onFinish(String response,final String flag) {
		boolean result = false;
//		Log.e("test", "response--"+flag+"="+response);
		if(flag.equals("province")){
			result = Utility.handleProvinceResponse(coolWeatherDB, response);
		}else if(flag.equals("city")){
			result = Utility.handleCitiesResponse(coolWeatherDB, response, selectedProvince.getId());
		}else if(flag.equals("county")){
			result = Utility.handleCountiesResponse(coolWeatherDB, response, selectedCity.getId());
		}
//		Log.e("test", "result--"+result);
		if(result){
			//通过runOnUiThread()方法回到主线程处理逻辑
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					colseProgressDialog();
					if(flag.equals("province")){
						queryProvinces();
					}else if(flag.equals("city")){
						queryCities();
					}else if(flag.equals("county")){
						queryCounties();
					}
				}
			});
		}
	}
	@Override
	public void onError(Exception e,String flag) {
		//通过runOnUiThread()方法回到主线程处理逻辑
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				colseProgressDialog();
				Toast.makeText(ChooseAreaActivity.this,"加载失败",Toast.LENGTH_SHORT).show();
			}
		});
	}
	/**
	 * 捕获Back按键，根据当前级别来判断，此时应该返回市列表、省列表、还是直接退出
	 */
	@Override
	public void onBackPressed() {
		if(currentLevel == LEVEL_COUNTY){
			queryCities();
		}else if(currentLevel == LEVEL_CITY){
			queryProvinces();
		}else{
			finish();
		}
	}
}