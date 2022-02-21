package ru.geekbrains.android2.mapapp.view

import android.content.Context
import android.os.Handler
import android.util.DisplayMetrics
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*

class RouteAnimateAdapter(context: Context?) {
    private var mAnimateListener: OnAnimateListener? = null
    private var animateMarkerPosition: LatLng? = null
    private var beginPosition: LatLng? = null
    private var endPosition: LatLng? = null
    private var animatePositionList: ArrayList<LatLng>? = null
    var animateMarker: Marker? = null
        private set
    var animatePolyline: Polyline? = null
        private set
    private var gm: GoogleMap? = null
    private var step = -1
    private var animateSpeed = -1
    private var zoom = -1
    private var animateDistance = -1.0
    private var animateCamera = -1.0
    private var totalAnimateDistance = 0.0
    private var cameraLock = false
    private var drawMarker = false
    private var drawLine = false
    private var flatMarker = false
    private var isCameraTilt = false
    private var isCameraZoom = false
    var isAnimated = false
        private set
    private var mContext: Context? = null

    fun setOnAnimateListener(listener: OnAnimateListener?) {
        mAnimateListener = listener
    }

    interface OnAnimateListener {
        fun onFinish()
        fun onStart()
        fun onProgress(progress: Int, total: Int)
    }

    fun animateDirection(
        gm: GoogleMap,
        direction: ArrayList<LatLng>,
        speed: Int,
        cameraLock: Boolean,
        isCameraTilt: Boolean,
        isCameraZoom: Boolean,
        drawMarker: Boolean,
        mo: MarkerOptions?,
        flatMarker: Boolean,
        drawLine: Boolean,
        po: PolylineOptions?
    ) {
        if (direction.size > 1) {
            isAnimated = true
            animatePositionList = direction
            animateSpeed = speed
            this.drawMarker = drawMarker
            this.drawLine = drawLine
            this.flatMarker = flatMarker
            this.isCameraTilt = isCameraTilt
            this.isCameraZoom = isCameraZoom
            step = 0
            this.cameraLock = cameraLock
            this.gm = gm
            setCameraUpdateSpeed(speed)
            beginPosition = animatePositionList!![step]
            endPosition = animatePositionList!![step + 1]
            animateMarkerPosition = beginPosition
            if (mAnimateListener != null) mAnimateListener!!.onProgress(
                step,
                animatePositionList!!.size
            )
            if (cameraLock) {
                val bearing = getBearing(beginPosition, endPosition)
                val cameraBuilder = CameraPosition.Builder()
                    .target(animateMarkerPosition!!).bearing(bearing)
                if (isCameraTilt) cameraBuilder.tilt(90f) else cameraBuilder.tilt(gm.cameraPosition.tilt)
                if (isCameraZoom) cameraBuilder.zoom(zoom.toFloat()) else cameraBuilder.zoom(gm.cameraPosition.zoom)
                val cameraPosition = cameraBuilder.build()
                gm.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }
            if (drawMarker) {
                animateMarker =
                    if (mo != null) gm.addMarker(mo.position(beginPosition!!)) else gm.addMarker(
                        MarkerOptions().position(
                            beginPosition!!
                        )
                    )
                if (flatMarker) {
                    animateMarker!!.isFlat = true
                    val rotation = getBearing(animateMarkerPosition, endPosition) + 180
                    animateMarker!!.rotation = rotation
                }
            }
            if (drawLine) {
                if (po != null) animatePolyline = gm.addPolyline(
                    po.add(beginPosition)
                        .add(beginPosition).add(endPosition)
                        .width(dpToPx(po.width.toInt()).toFloat())
                ) else animatePolyline = gm.addPolyline(
                    PolylineOptions()
                        .width(dpToPx(5).toFloat())
                )
            }
            Handler().postDelayed(r, speed.toLong())
            if (mAnimateListener != null) mAnimateListener!!.onStart()
        }
    }

    fun cancelAnimated() {
        isAnimated = false
    }

    private val r: Runnable = object : Runnable {
        override fun run() {
            animateMarkerPosition = getNewPosition(animateMarkerPosition, endPosition)
            if (drawMarker) animateMarker!!.position = animateMarkerPosition!!
            if (drawLine) {
                val points = animatePolyline!!.points
                points.add(animateMarkerPosition)
                animatePolyline!!.points = points
            }
            if (animateMarkerPosition!!.latitude == endPosition!!.latitude
                && animateMarkerPosition!!.longitude == endPosition!!.longitude
            ) {
                if (step == animatePositionList!!.size - 2) {
                    isAnimated = false
                    totalAnimateDistance = 0.0
                    if (mAnimateListener != null) mAnimateListener!!.onFinish()
                } else {
                    step++
                    beginPosition = animatePositionList!![step]
                    endPosition = animatePositionList!![step + 1]
                    animateMarkerPosition = beginPosition
                    if (flatMarker && step + 3 < animatePositionList!!.size - 1) {
                        val rotation =
                            getBearing(animateMarkerPosition, animatePositionList!![step + 3]) + 180
                        animateMarker!!.rotation = rotation
                    }
                    if (mAnimateListener != null) mAnimateListener!!.onProgress(
                        step,
                        animatePositionList!!.size
                    )
                }
            }
            if (cameraLock && (totalAnimateDistance > animateCamera || !isAnimated)) {
                totalAnimateDistance = 0.0
                val bearing = getBearing(beginPosition, endPosition)
                val cameraBuilder = CameraPosition.Builder()
                    .target(animateMarkerPosition!!).bearing(bearing)
                if (isCameraTilt) cameraBuilder.tilt(90f) else cameraBuilder.tilt(gm!!.cameraPosition.tilt)
                if (isCameraZoom) cameraBuilder.zoom(zoom.toFloat()) else cameraBuilder.zoom(gm!!.cameraPosition.zoom)
                val cameraPosition = cameraBuilder.build()
                gm!!.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }
            if (isAnimated) {
                Handler().postDelayed(this, animateSpeed.toLong())
            }
        }
    }

    private fun getNewPosition(begin: LatLng?, end: LatLng?): LatLng? {
        val lat = Math.abs(begin!!.latitude - end!!.latitude)
        val lng = Math.abs(begin.longitude - end.longitude)
        val dis = Math.sqrt(Math.pow(lat, 2.0) + Math.pow(lng, 2.0))
        return if (dis >= animateDistance) {
            var angle = -1.0
            if (begin.latitude <= end.latitude && begin.longitude <= end.longitude) angle =
                Math.toDegrees(Math.atan(lng / lat)) else if (begin.latitude > end.latitude && begin.longitude <= end.longitude) angle =
                90 - Math.toDegrees(Math.atan(lng / lat)) + 90 else if (begin.latitude > end.latitude && begin.longitude > end.longitude) angle =
                Math.toDegrees(Math.atan(lng / lat)) + 180 else if (begin.latitude <= end.latitude && begin.longitude > end.longitude) angle =
                90 - Math.toDegrees(Math.atan(lng / lat)) + 270
            val x = Math.cos(Math.toRadians(angle)) * animateDistance
            val y = Math.sin(Math.toRadians(angle)) * animateDistance
            totalAnimateDistance += animateDistance
            val finalLat = begin.latitude + x
            val finalLng = begin.longitude + y
            LatLng(finalLat, finalLng)
        } else {
            end
        }
    }

    private fun getBearing(begin: LatLng?, end: LatLng?): Float {
        val lat = Math.abs(begin!!.latitude - end!!.latitude)
        val lng = Math.abs(begin.longitude - end.longitude)
        if (begin.latitude < end.latitude && begin.longitude < end.longitude) return Math.toDegrees(
            Math.atan(lng / lat)
        )
            .toFloat() else if (begin.latitude >= end.latitude && begin.longitude < end.longitude) return (90 - Math.toDegrees(
            Math.atan(lng / lat)
        ) + 90).toFloat() else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude) return (Math.toDegrees(
            Math.atan(lng / lat)
        ) + 180).toFloat() else if (begin.latitude < end.latitude && begin.longitude >= end.longitude) return (90 - Math.toDegrees(
            Math.atan(lng / lat)
        ) + 270).toFloat()
        return (-1).toFloat()
    }

    fun setCameraUpdateSpeed(speed: Int) {
        if (speed == SPEED_VERY_SLOW) {
            animateDistance = 0.000005
            animateSpeed = 20
            animateCamera = 0.0004
            zoom = 19
        } else if (speed == SPEED_SLOW) {
            animateDistance = 0.00001
            animateSpeed = 20
            animateCamera = 0.0008
            zoom = 18
        } else if (speed == SPEED_NORMAL) {
            animateDistance = 0.00005
            animateSpeed = 20
            animateCamera = 0.002
            zoom = 16
        } else if (speed == SPEED_FAST) {
            animateDistance = 0.0001
            animateSpeed = 20
            animateCamera = 0.004
            zoom = 15
        } else if (speed == SPEED_VERY_FAST) {
            animateDistance = 0.0005
            animateSpeed = 20
            animateCamera = 0.004
            zoom = 13
        } else {
            animateDistance = 0.00005
            animateSpeed = 20
            animateCamera = 0.002
            zoom = 16
        }
    }

    private fun dpToPx(dp: Int): Int {
        val displayMetrics = mContext!!.resources.displayMetrics
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))
    }

    companion object {
        const val MODE_DRIVING = "driving"
        const val MODE_WALKING = "walking"
        const val MODE_BICYCLING = "bicycling"
        const val SPEED_VERY_FAST = 1
        const val SPEED_FAST = 2
        const val SPEED_NORMAL = 3
        const val SPEED_SLOW = 4
        const val SPEED_VERY_SLOW = 5
    }

    init {
        mContext = context
    }
}