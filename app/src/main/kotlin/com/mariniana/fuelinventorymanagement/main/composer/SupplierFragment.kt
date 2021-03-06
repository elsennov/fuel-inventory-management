package com.mariniana.fuelinventorymanagement.main.composer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.RxView
import com.mariniana.fuelinventorymanagement.MyApplication
import com.mariniana.fuelinventorymanagement.R
import com.mariniana.fuelinventorymanagement.main.model.Refill
import com.mariniana.fuelinventorymanagement.main.presenter.MainPresenter
import com.mariniana.fuelinventorymanagement.utils.LogUtils
import com.trello.navi2.Event
import com.trello.navi2.component.support.NaviFragment
import com.trello.navi2.rx.RxNavi
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_supplier.*

/**
 * Created by elsennovraditya on 4/2/17.
 */
class SupplierFragment : NaviFragment() {

    companion object {
        private val TAG = SupplierFragment::class.java.simpleName
        const val REFILL_ID = "refill_id"

        fun getInstance(refillId: String): SupplierFragment {
            val bundle = Bundle()
            bundle.putString(REFILL_ID, refillId)

            val supplierFragment = SupplierFragment()
            supplierFragment.arguments = bundle
            return supplierFragment
        }
    }

    private val naviComponent = this
    private val mainPresenter: MainPresenter by lazy {
        MyApplication.fimComponent.provideMainPresenter()
    }

    init {
        initListenToRefillRequest()
        initSendFuel()
    }

    private fun initListenToRefillRequest() {
        RxNavi
            .observe(naviComponent, Event.VIEW_CREATED)
            .observeOn(Schedulers.io())
            .flatMap { mainPresenter.listenToRefillRequestObservable() }
            .observeOn(AndroidSchedulers.mainThread())
            .takeUntil(RxNavi.observe(naviComponent, Event.DESTROY_VIEW))
            .subscribe(
                {
                    LogUtils.debug(TAG, "onNext in initListenToRefillRequest")
                    send_fuel.isEnabled = true
                },
                { LogUtils.error(TAG, "onError in initListenToRefillRequest", it) },
                { LogUtils.debug(TAG, "onComplete in initListenToRefillRequest") }
            )
    }

    private fun initSendFuel() {
        RxNavi
            .observe(naviComponent, Event.VIEW_CREATED)
            .observeOn(Schedulers.io())
            .map { arguments.getString(REFILL_ID, "") }
            .flatMap {
                mainPresenter
                    .getRefillObservable(it)
                    .onErrorResumeNext(Function { Observable.just(Refill.empty) })
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { send_fuel.isEnabled = it.status == Refill.REQUESTED }
            .flatMap {
                RxView
                    .clicks(send_fuel)
                    .observeOn(Schedulers.io())
                    .flatMap {
                        mainPresenter
                            .getRefillObservable("")
                            .onErrorResumeNext(Function { Observable.just(Refill.empty) })
                    }
                    .flatMap {
                        mainPresenter
                            .sendFuelObservable(it.id ?: "")
                            .onErrorResumeNext(Function { Observable.just(false) })
                    }
                    .filter { it }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .takeUntil(RxNavi.observe(naviComponent, Event.DESTROY_VIEW))
            .subscribe(
                {
                    LogUtils.debug(TAG, "onNext in initSendFuel")
                    send_fuel.isEnabled = false
                },
                { LogUtils.error(TAG, "onError in initSendFuel", it) },
                { LogUtils.debug(TAG, "onComplete in initSendFuel") }
            )
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater?.inflate(R.layout.fragment_supplier, container, false)
    }

}