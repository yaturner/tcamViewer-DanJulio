package com.danjuliodesigns.tcamViewer.ui.settings;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavDirections;

import com.danjuliodesigns.tcamViewer.BuildConfig;
import com.danjuliodesigns.tcamViewer.MainActivity;
import com.danjuliodesigns.tcamViewer.R;
import com.danjuliodesigns.tcamViewer.databinding.FragmentPrivacyDisclosureBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.sentry.Sentry;

public class PrivacyDisclosureFragment extends Fragment implements MenuProvider {
    private MainActivity mainActivity;
    private FragmentPrivacyDisclosureBinding binding;
    private BottomNavigationView navBar;
    private View root;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = MainActivity.getInstance();
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        mainActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        binding = FragmentPrivacyDisclosureBinding.inflate(inflater, container, false);
        if(savedInstanceState != null) {
            binding.wvDisclosure.restoreState(savedInstanceState);
        }
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        root = binding.getRoot();
        return root;
    }

    public void onSaveInstanceState(Bundle outState) {
        binding.wvDisclosure.saveState(outState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String htmlString = readTxt();
        binding.wvDisclosure.setWebChromeClient(new WebChromeClient());
        binding.wvDisclosure.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        binding.wvDisclosure.getSettings().setUseWideViewPort(false);
        binding.wvDisclosure.loadDataWithBaseURL(null, htmlString, "text/html", "ISO-8859-1", null);
        navBar = getActivity().findViewById(R.id.nav_view);
        if(navBar != null) {
            navBar.setVisibility(View.GONE);
        }
    }

    private String readTxt() {
        InputStream inputStream = getResources().openRawResource(R.raw.privacy);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int i;
        try {
            i = inputStream.read();
            while (i != -1) {
                byteArrayOutputStream.write(i);
                i = inputStream.read();
            }
            inputStream.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Sentry.captureException(e);
        }
        //update version
        String string = byteArrayOutputStream.toString();
        string = string.replace("x.x", BuildConfig.VERSION_NAME);
        return string;
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {

    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        // command switch
        int id = menuItem.getItemId();
        if(id == android.R.id.home) {
            NavDirections navDirections = PrivacyDisclosureFragmentDirections.
                    actionPrivacyDisclosureToNavigationSettings();
            mainActivity.getNavController().navigate(navDirections);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
