package com.youtubeapis

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.youtubeapis.databinding.ActivityStartBinding
import com.youtubeapis.decorHome.DecorHomeActivity
import com.youtubeapis.decorHome.TestActivity
import com.youtubeapis.files.FileActivity
import com.youtubeapis.flash.FlashActivity
import com.youtubeapis.stack.StackActivity
import com.youtubeapis.ui.WebActivity
import com.youtubeapis.vpn.VPNActivity

class StartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnFile.setOnClickListener {
            startActivity(Intent(this, FileActivity::class.java))
        }
        binding.btnFlash.setOnClickListener {
            startActivity(Intent(this, FlashActivity::class.java))
        }
        binding.btnWeb.setOnClickListener {
            startActivity(Intent(this, WebActivity::class.java))
        }
        binding.btnWheel.setOnClickListener {
            startActivity(Intent(this, LuckyWheelActivity::class.java))
        }
        binding.btnMain.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        binding.btnStack.setOnClickListener {
            startActivity(Intent(this, StackActivity::class.java))
        }
        binding.btnDecor.setOnClickListener {
            startActivity(Intent(this, DecorHomeActivity::class.java))
        }
        binding.btnVPN.setOnClickListener {
            startActivity(Intent(this, VPNActivity::class.java))
        }

       binding.btnOther.setOnClickListener {
            startActivity(Intent(this, TestActivity::class.java))
        }

    }

}