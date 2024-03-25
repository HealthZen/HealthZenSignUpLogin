package com.example

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.healthzensignuplogin.R



/**
 * A simple [Fragment] subclass.
 * Use the [StressReliefFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class StressReliefFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stress_relief, container, false)

        val toBreathingExercisesButton=view.findViewById<Button>(R.id.button_BreathingExercises)



        toBreathingExercisesButton.setOnClickListener {
            // Navigate to the EducationFragment when the button is clicked
            navigateToBreathingExercisesFragment()
        }



//        toEducationButton.setOnClickListener {
//            findNavController().navigate(R.id.Education)
//        }

        // Inflate the layout for this fragment


        return view
    }

    private fun navigateToBreathingExercisesFragment() {
        // Create instance of EducationFragment
        val breathingExercisesFragment = BreathingExercisesFragment()

        // Get FragmentManager
        val fragmentManager: FragmentManager = requireActivity().supportFragmentManager

        // Start a FragmentTransaction
        val transaction: FragmentTransaction = fragmentManager.beginTransaction()

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack
        transaction.replace(R.id.frame_container, breathingExercisesFragment)
        transaction.addToBackStack(null)

        // Commit the transaction
        transaction.commit()
    }}

