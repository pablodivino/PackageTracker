package info.papdt.express.helper.ui.fragment.add

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.support.design.widget.TextInputEditText
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import cn.nekocode.rxlifecycle.RxLifecycle
import info.papdt.express.helper.R
import info.papdt.express.helper.REQUEST_CODE_CHOOSE_COMPANY
import info.papdt.express.helper.RESULT_EXTRA_COMPANY_CODE
import info.papdt.express.helper.api.Kuaidi100PackageApi
import info.papdt.express.helper.api.RxPackageApi
import info.papdt.express.helper.dao.PackageDatabase
import info.papdt.express.helper.model.BaseMessage
import info.papdt.express.helper.model.Kuaidi100Package
import info.papdt.express.helper.receiver.ConnectivityReceiver
import info.papdt.express.helper.support.PackageApiType
import info.papdt.express.helper.support.SettingsInstance
import info.papdt.express.helper.ui.CompanyChooserActivity
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import moe.feng.common.stepperview.VerticalStepperItemView
import moe.feng.kotlinyan.common.*

class NewAddDialogFragment : Fragment() {

    companion object {

        private const val ARG_NUMBER = "number"

        fun newInstance(number: String) = AddDialogFragment().apply {
            arguments = Bundle().apply { putString(ARG_NUMBER, number) }
        }

    }

    private lateinit var step0: VerticalStepperItemView
    private lateinit var step1: VerticalStepperItemView
    private lateinit var step2: VerticalStepperItemView
    private lateinit var currentCompanyText: TextView
    private lateinit var step1NextButton: Button
    private lateinit var loadingLayout: View
    private lateinit var detectErrorView: View
    private lateinit var detectTryAgainButton: Button
    private lateinit var addErrorView: View
    private lateinit var addErrorMsg: TextView
    private lateinit var addErrorDesc: TextView
    private lateinit var addLoadingView: View
    private lateinit var addFinishLayout: View
    private lateinit var nameEdit: TextInputEditText

    private var currentStep = 0

    private var companyCode: String = ""

    private var result: Kuaidi100Package? = null

    private val number get() = arguments!!.getString(ARG_NUMBER) ?: "1234"

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_add_dialog, container, false)

        step0 = view.findViewById(R.id.stepper_detect_company)
        step1 = view.findViewById(R.id.stepper_choose_company)
        step2 = view.findViewById(R.id.stepper_find_package)
        currentCompanyText = view.findViewById(R.id.tv_current_company)
        step1NextButton = view.findViewById(R.id.choose_company_next_btn)
        loadingLayout = view.findViewById(R.id.detecting_layout)
        detectErrorView = view.findViewById(R.id.error_text)
        detectTryAgainButton = view.findViewById(R.id.stepper_try_again)
        addErrorView = view.findViewById(R.id.error_layout_add)
        addErrorMsg = view.findViewById(R.id.add_error_message_text)
        addErrorDesc = view.findViewById(R.id.add_error_desc_text)
        addLoadingView = view.findViewById(R.id.loading_layout_add)
        addFinishLayout = view.findViewById(R.id.set_name_layout)
        nameEdit = view.findViewById(R.id.name_edit)

        VerticalStepperItemView.bindSteppers(step0, step1, step2)

        step0.summary = resources.string[R.string.stepper_detect_company_summary].format(number)
        step1.summary = when (SettingsInstance.packageApiTypeInt) {
            PackageApiType.BAIDU -> resources.string[R.string.summary_baidu_not_support_choose_company]
            else -> null
        }

        step1NextButton.setOnClickListener { step1.nextStep() }
        view.findViewById<Button>(R.id.choose_company_change_btn).setOnClickListener {
            val intent = Intent(activity, CompanyChooserActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            startActivityForResult(intent, REQUEST_CODE_CHOOSE_COMPANY)
        }
        detectTryAgainButton.setOnClickListener { doStep() }
        view.findViewById<Button>(R.id.choose_company_next_btn).setOnClickListener {
            currentStep = 2
            step1.nextStep()
            doStep()
        }
        view.findViewById<Button>(R.id.try_again_btn_step_2).setOnClickListener { doStep() }
        view.findViewById<Button>(R.id.back_button_step_2).apply {
            if (SettingsInstance.packageApiTypeInt == PackageApiType.BAIDU) {
                isEnabled = false
            }
            setOnClickListener {
                step2.prevStep()
            }
        }
        view.findViewById<Button>(R.id.back_button_step_2_2).apply {
            if (SettingsInstance.packageApiTypeInt == PackageApiType.BAIDU) {
                isEnabled = false
            }
            setOnClickListener {
                step2.prevStep()
            }
        }
        view.findViewById<Button>(R.id.stepper_add_button).setOnClickListener {
            result?.name = if (nameEdit.text!!.isNotBlank())
                nameEdit.text.toString() else String.format(getString(R.string.package_name_unnamed),
                    if (number.length >= 4) number?.substring(0, 4) else number)
            Observable.just(result!!)
                    .compose(RxLifecycle.bind(activity!!).withObservable())
                    .map { newData ->
                        val db = PackageDatabase.getInstance(context!!)
                        db.add(newData)
                        db.save()
                    }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { _ ->
                        detectErrorView.makeGone()
                        detectTryAgainButton.makeGone()
                        loadingLayout.makeVisible()
                    }
                    .subscribe { _ ->
                        (activity as? IAddDialogObserver)?.onPackageAdd(result!!)
                    }
        }

        doStep()

        return view
    }

    private fun doStep() {
        when (currentStep) {
            0 -> {
                if (ConnectivityReceiver.readNetworkState(activity)) {
                    RxPackageApi.detectCompany(number, activity)
                            .doOnSubscribe {
                                detectErrorView.makeGone()
                                detectTryAgainButton.makeGone()
                                loadingLayout.makeVisible()
                            }
                            .subscribe {
                                step0.setErrorText(0)
                                when (SettingsInstance.packageApiTypeInt) {
                                    PackageApiType.BAIDU -> {
                                        currentStep = 2
                                        step0.state = VerticalStepperItemView.STATE_DONE
                                        step1.nextStep()
                                        doStep()
                                    }
                                    else -> {
                                        currentStep = 1
                                        step0.nextStep()
                                    }
                                }
                                it?.let(this::setCompany)
                            }
                } else {
                    step0.setErrorText(R.string.message_no_internet_connection)
                    detectErrorView.makeVisible()
                    detectTryAgainButton.makeVisible()
                    loadingLayout.makeGone()
                }
            }
            2 -> {
                if (ConnectivityReceiver.readNetworkState(activity)) {
                    RxPackageApi.getPackage(number, companyCode, activity)
                            .doOnSubscribe {
                                addLoadingView.makeVisible()
                                addErrorView.makeGone()
                                addFinishLayout.makeGone()
                                step2.setErrorText(0)
                            }
                            .subscribe {
                                if (it.code == BaseMessage.CODE_OKAY && it.data?.getState() != Kuaidi100Package.STATUS_FAILED) {
                                    addErrorView.makeGone()
                                    addLoadingView.makeGone()
                                    addFinishLayout.makeVisible()
                                    result = it.data
                                    view!!.findViewById<TextView>(R.id.add_message_text).text =
                                            resources.string[R.string.message_successful_format]
                                                    .format(number, currentCompanyText.text)
                                } else {
                                    addErrorView.makeVisible()
                                    addLoadingView.makeGone()
                                    addFinishLayout.makeGone()
                                    addErrorMsg.setText(R.string.message_no_found)
                                    addErrorDesc.setText(R.string.description_no_found)
                                    step2.setErrorText(R.string.message_no_found)
                                }
                            }
                } else {
                    addLoadingView.makeGone()
                    addErrorView.makeVisible()
                    addFinishLayout.makeGone()
                    addErrorMsg.setText(R.string.message_no_internet_connection)
                    addErrorDesc.setText(R.string.description_no_internet_connection)
                    step2.setErrorText(R.string.message_no_internet_connection)
                }
            }
        }
    }

    private fun setCompany(company: String) {
        this.companyCode = company
        currentCompanyText.text = if (company.isNotBlank())
            Kuaidi100PackageApi.CompanyInfo.getNameByCode(company) else resources.string[R.string.stepper_company_cannot_detect]
        step1NextButton.isEnabled = company.isNotBlank()
        step1NextButton.backgroundTintList = ColorStateList.valueOf(
                if (company.isNotBlank())
                    resources.color[R.color.colorAccent] else resources.color[R.color.grey_300]
        )
        step1.setErrorText(if (company.isBlank()) R.string.stepper_company_cannot_detect else 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == REQUEST_CODE_CHOOSE_COMPANY) {
            if (resultCode == Activity.RESULT_OK) {
                intent!![RESULT_EXTRA_COMPANY_CODE]?.asString()?.let(this::setCompany)
            }
        }
    }

    interface IAddDialogObserver {

        fun onPackageAdd(p: Kuaidi100Package)

    }

}