package kr.co.connect.boostcamp.livewhere.ui.map

import android.graphics.PointF
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.CircleOverlay
import com.naver.maps.map.overlay.Marker
import io.reactivex.Single
import kr.co.connect.boostcamp.livewhere.R
import kr.co.connect.boostcamp.livewhere.model.*
import kr.co.connect.boostcamp.livewhere.repository.MapRepositoryImpl
import kr.co.connect.boostcamp.livewhere.util.MapUtilImpl
import kr.co.connect.boostcamp.livewhere.util.RADIUS
import kr.co.connect.boostcamp.livewhere.util.StatusCode

interface OnMapViewModelInterface : NaverMap.OnMapLongClickListener, OnMapReadyCallback, View.OnClickListener,
    OnMapHistoryListener, OnSearchTrigger

class MapViewModel(val mapUtilImpl: MapUtilImpl, val mapRepository: MapRepositoryImpl) : ViewModel(),
    OnMapViewModelInterface {

    //현재 검색하려는 매물의 좌표 livedata
    private val _markerLiveData: MutableLiveData<MarkerInfo> = MutableLiveData()
    val markerLiveData: LiveData<MarkerInfo>
        get() = _markerLiveData

    private val _filterMarkerLiveData: MutableLiveData<MarkerInfo> = MutableLiveData()
    //현재 검색하려는 house의 좌표 livedata
    val filterMarkerLiveData: LiveData<MarkerInfo>
        get() = _filterMarkerLiveData

    //현재 사용하고 있는 naverMap의 status livedata
    private val _mapStatusLiveData: MutableLiveData<NaverMap> = MutableLiveData()
    val mapStatusLiveData: LiveData<NaverMap>
        get() = _mapStatusLiveData

    //상권 정보에 대한 LiveData
    private val _placeResponseLiveData: MutableLiveData<PlaceResponse> = MutableLiveData()
    val placeResponseLiveData: LiveData<PlaceResponse>
        get() = _placeResponseLiveData

    //하나의 상권의 정보를 가져오기 위한 LiveData
    private val _placeMarkerLiveData: MutableLiveData<Place> = MutableLiveData()
    val placeMarkerLiveData: LiveData<Place>
        get() = _placeMarkerLiveData

    //결과 값을 보여주는 RecyclerView에 들어가는 LiveData,
    //List<Place>, List<House>, List<Empty>의 data가 들어옴
    private val _searchListLiveData: MutableLiveData<List<Any>> = MutableLiveData()
    val searchListLiveData: LiveData<List<Any>>
        get() = _searchListLiveData

    //사용자의 상태를 체크하는 liveData, 값에 따라서 백드롭의 타이틀 변경에 사용
    private val _userStatusLiveData: MutableLiveData<UserStatus> = MutableLiveData()
    val userStatusLiveData: LiveData<UserStatus>
        get() = _userStatusLiveData

    //삭제할 PlaceMarker Livedata
    private val _removePlaceMarkersLiveData: MutableLiveData<MutableList<Marker>> = MutableLiveData()

    //PlaceMarker의 정보를 저장해두는 LiveData
    private val _savePlaceMarkersLiveData: MutableLiveData<MutableList<Marker>> = MutableLiveData()
    val removePlaceMarkersLiveData: LiveData<MutableList<Marker>>
        get() = _removePlaceMarkersLiveData

    //이전 overlay의 정보를 갖고 있는 LiveData
    private val _tempOverlayLiveData: MutableLiveData<CircleOverlay> = MutableLiveData()
    val tempOverlayLiveData: LiveData<CircleOverlay>
        get() = _tempOverlayLiveData

    //현재 overlay의 정보를 갖고 있는 LiveData
    private val _currentOverlayLiveData: MutableLiveData<CircleOverlay> = MutableLiveData()
    val currentOverlayLiveData: LiveData<CircleOverlay>
        get() = _currentOverlayLiveData

    override fun onSaveCircleOverlay(circleOverlay: CircleOverlay) {
        if (circleOverlay != _currentOverlayLiveData.value) {
            _currentOverlayLiveData.postValue(circleOverlay)
        }
    }

    override fun onRemoveCircleOverlay() {
        _tempOverlayLiveData.postValue(currentOverlayLiveData.value)
    }


    override fun onDrawCircleOverlay(currentOverlay: CircleOverlay) {
        _tempOverlayLiveData.postValue(_tempOverlayLiveData.value)
        _tempOverlayLiveData.postValue(currentOverlay)
        _currentOverlayLiveData.postValue(currentOverlay)
    }

    override fun onSaveFilterMarker(markerList: MutableList<Marker>) {
        _savePlaceMarkersLiveData.postValue(markerList)
    }

    override fun onRemoveFilterMarker() {
        _removePlaceMarkersLiveData.postValue(_savePlaceMarkersLiveData.value)
    }

    override fun searchTrigger(view: View) {
        if (view is BackdropMotionLayout) {

        }
    }

    override fun onClickMarkerPlace(place: Place) {
        _placeMarkerLiveData.postValue(place)
    }

    override fun onClickMarkerHouse(house: MarkerInfo) {
        _markerLiveData.postValue(house)
    }

    override fun onClick(view: View?) {
        _userStatusLiveData.postValue(UserStatus(StatusCode.BEFORE_SEARCH_PLACE, ""))
        val currentMarkerInfo = markerLiveData.value
        val category = when {
            view?.id == R.id.fab_filter_cafe -> "CE7"
            view?.id == R.id.fab_filter_food -> "FD6"
            view?.id == R.id.fab_filter_hospital -> "HP8"
            view?.id == R.id.fab_filter_school -> "PS3,SC4"
            view?.id == R.id.fab_filter_store -> "MT1,CS2"
            else -> ""
        }

        if (currentMarkerInfo != null) {
            val latLng = currentMarkerInfo.latLng
            mapRepository.getPlace(latLng.latitude, latLng.longitude, RADIUS, category).subscribe({ response ->
                val placeResponse = response.body()
                val placeList = placeResponse?.placeList
                _placeResponseLiveData.postValue(placeResponse)
                _searchListLiveData.postValue(placeList)
                _userStatusLiveData.postValue(
                    UserStatus(
                        StatusCode.SUCCESS_SEARCH_PLACE, String.format(
                            view?.context!!.getString(R.string.info_success_search_place_text),
                            placeList!![0].category,
                            placeList.size
                        )
                    )
                )
            }, {
                _userStatusLiveData.postValue(UserStatus(StatusCode.FAILURE_SEARCH_PLACE, ""))
            })
        }
    }

    override fun onMapLongClick(point: PointF, latLng: LatLng) {
        _userStatusLiveData.postValue(UserStatus(StatusCode.SEARCH_HOUSE, "${latLng.latitude}, ${latLng.longitude}"))
        loadHousePrice(latLng)
    }

    override fun onMapReady(naverMap: NaverMap) {
        _mapStatusLiveData.postValue(naverMap)
        _userStatusLiveData.postValue(UserStatus(StatusCode.DEFAULT_SEARCH, ""))
    }

    private fun loadHousePrice(latLng: LatLng) = mapRepository
        .getAddress(latLng.latitude.toString(), latLng.longitude.toString(), "WGS84")
        .flatMap { address ->
            if (address.isSuccessful && address.body()?.metaData?.total_count!! >= 1) {
                mapRepository.getHouseDetail(address.body()?.documentData!![0].addressMeta.addressName)
                    .map { response -> Pair(response, address.body()?.documentData!![0].addressMeta.addressName) }
            } else {
                mapRepository.getHouseDetail(address.body()?.metaData?.total_count.toString())
                    .map { response -> Pair(response, address.body()?.documentData!![0].addressMeta.addressName) }
            }
        }
        .flatMap { pair ->
            val response = pair.first
            val address = pair.second
            val houseResponse = response.body()
            if (houseResponse?.addrStatusCode == StatusCode.RESULT_200.response
                && houseResponse.houseStatusCode == StatusCode.RESULT_200.response
            ) {
                Single.just(Pair(address, houseResponse))
            } else {
                Single.just(Pair(address, null))
            }
        }
        .subscribe({ result ->
            val address = result.first
            val response = result.second
            if (response != null) {
                val houseList = response.houseList
                val currentMarkerInfo = MarkerInfo(latLng, houseList, StatusCode.RESULT_200)
                _markerLiveData.postValue(currentMarkerInfo)
                _userStatusLiveData.postValue(UserStatus(StatusCode.SUCCESS_SEARCH_HOUSE, houseList[0].name))
                _searchListLiveData.postValue(houseList)
            } else {
                val currentMarkerInfo = MarkerInfo(latLng, emptyList(), StatusCode.RESULT_204)
                _userStatusLiveData.postValue(UserStatus(StatusCode.EMPTY_SEARCH_HOUSE, ""))
                _searchListLiveData.postValue(listOf(EmptyInfo(address)))
                _markerLiveData.postValue(currentMarkerInfo)
            }
        }, {
            _userStatusLiveData.postValue(UserStatus(StatusCode.FAILURE_SEARCH_HOUSE, ""))
        })
}