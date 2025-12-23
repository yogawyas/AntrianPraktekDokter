package com.example.antrianpraktekdokter.admin

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.antrianpraktekdokter.R
import com.example.antrianpraktekdokter.admin.viewmodel.JadwalViewModel
import java.util.Calendar

class AturJadwalFragment : Fragment() {

    private lateinit var viewModel: JadwalViewModel
    private lateinit var switchStatusPraktik: Switch
    private lateinit var etJamBuka: EditText
    private lateinit var etJamTutup: EditText
    private lateinit var btnSimpanJadwal: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_atur_jadwal, container, false)


        viewModel = ViewModelProvider(this)[JadwalViewModel::class.java]


        switchStatusPraktik = view.findViewById(R.id.switchStatusPraktik)
        etJamBuka = view.findViewById(R.id.etJamBuka)
        etJamTutup = view.findViewById(R.id.etJamTutup)
        btnSimpanJadwal = view.findViewById(R.id.btnSimpanJadwal)


        viewModel.jadwalConfig.observe(viewLifecycleOwner) { config ->

            switchStatusPraktik.setOnCheckedChangeListener(null)

            switchStatusPraktik.isChecked = config.isOpen
            etJamBuka.setText(config.bukaJam)
            etJamTutup.setText(config.tutupJam)


            setupSwitchListener()
        }

        viewModel.message.observe(viewLifecycleOwner) { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }


        setupSwitchListener()

        btnSimpanJadwal.setOnClickListener {
            viewModel.saveJadwal(etJamBuka.text.toString(), etJamTutup.text.toString())
        }

        etJamBuka.setOnClickListener { showTimePicker(etJamBuka) }
        etJamTutup.setOnClickListener { showTimePicker(etJamTutup) }

        viewModel.loadJadwal()

        return view
    }

    private fun setupSwitchListener() {
        switchStatusPraktik.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateStatus(isChecked)
        }
    }

    private fun showTimePicker(et: EditText) {
        val c = Calendar.getInstance()
        TimePickerDialog(requireContext(), { _, h, m ->
            et.setText(String.format("%02d:%02d", h, m))
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
    }
}