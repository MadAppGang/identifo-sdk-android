package com.prytula.identifolibui.login.username

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.prytula.IdentifoAuth
import com.prytula.identifolib.extensions.onError
import com.prytula.identifolib.extensions.onSuccess
import com.prytula.identifolibui.R
import com.prytula.identifolibui.databinding.FragmentLoginUsernameBinding
import com.prytula.identifolibui.databinding.FragmentPhoneNumberLoginBinding
import com.prytula.identifolibui.extensions.makeUnderline
import com.prytula.identifolibui.extensions.onDone
import com.prytula.identifolibui.extensions.showMessage


/*
 * Created by Eugene Prytula on 3/4/21.
 * Copyright (c) 2021 MadAppGang. All rights reserved.
 */

class UsernameLoginFragment : Fragment(R.layout.fragment_login_username) {

    private val usernameLoginBinding by viewBinding(FragmentLoginUsernameBinding::bind)
    private val usernameLoginViewModel: UsernameLoginViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        usernameLoginBinding.editTextPassword.onDone {
            usernameLoginViewModel.performLogin(
                usernameLoginBinding.editTextTextEmailAddress.text.toString(),
                usernameLoginBinding.editTextPassword.text.toString()
            )
        }
        usernameLoginBinding.buttonLogin.setOnClickListener {
            usernameLoginViewModel.performLogin(
                usernameLoginBinding.editTextTextEmailAddress.text.toString(),
                usernameLoginBinding.editTextPassword.text.toString()
            )
        }

        usernameLoginViewModel.signInSuccessful.asLiveData().observe(viewLifecycleOwner) {
            requireActivity().finish()
        }

        usernameLoginViewModel.receiveError.asLiveData().observe(viewLifecycleOwner) {
            usernameLoginBinding.constraintLoginRoot.showMessage(it.error.message)
        }

        with(usernameLoginBinding.textViewRegisterNewAccount) {
            text = getString(R.string.registerNewAccount).makeUnderline()
            setOnClickListener {
                findNavController().navigate(R.id.action_usernameLoginFragment_to_registrationFragment)
            }
        }

        with(usernameLoginBinding.textViewForgotThePassword) {
            text = getString(R.string.forgotThePassword).makeUnderline()
            setOnClickListener {
                findNavController().navigate(R.id.action_usernameLoginFragment_to_resetPasswordFragment)
            }
        }
    }
}