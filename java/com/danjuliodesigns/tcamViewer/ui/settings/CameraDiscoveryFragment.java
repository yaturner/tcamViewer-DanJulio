package com.danjuliodesigns.tcamViewer.ui.settings;

//mDNS Discovery
//        The cameras advertise themselves on the local network using mDNS (Bonjour) starting with
//        firmware revision 3.0 to make discovering their IPV4 addresses easier.
//
//        Service Type: "_tcam-socket._tcp."
//        Host/Instance Name: Camera Name (e.g. "tCam-Mini-87E9")
//        TXT Records:
//        "model": Camera model (e.g. "tCam", "tCam-Mini", "tCam-POE")
//        "interface": Communication interface (e.g. "Ethernet", "WiFi")
//        "version": Firmware version (e.g. "3.0")
//
// Reference: http://developer.android.com/training/connect-devices-wirelessly/nsd.html

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavDirections;

import com.danjuliodesigns.tcamViewer.MainActivity;
import com.danjuliodesigns.tcamViewer.R;
import com.danjuliodesigns.tcamViewer.adapters.CameraDiscoveryAdapter;
import com.danjuliodesigns.tcamViewer.constants.Constants;
import com.danjuliodesigns.tcamViewer.databinding.FragmentCameraDiscoveryBinding;
import com.danjuliodesigns.tcamViewer.model.Settings;
import com.danjuliodesigns.tcamViewer.model.SettingsViewModel;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import timber.log.Timber;

public class CameraDiscoveryFragment extends Fragment implements
        View.OnClickListener,
        AdapterView.OnItemClickListener,
        MenuProvider
{
    private NsdManager mNsdManager;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.ResolveListener mResolveListener;
    private NsdServiceInfo mServiceInfo;
    private String mCameraAddress;
    private MainActivity mainActivity = MainActivity.getInstance();
    private SettingsViewModel settingsViewModel;
    private FragmentCameraDiscoveryBinding binding;
    private ViewGroup container;
    private Settings settings;
    private ArrayList<Pair<String, String>> cameraArray;
    private CameraDiscoveryAdapter mCameraAdapter;
    private Semaphore resolveSemaphore;
    private String selectedIPAddess;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        this.container = container;
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

//        if (mainActivity.getSupportActionBar() != null) {
//            mainActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
//            mainActivity.getSupportActionBar().setHomeButtonEnabled(false);
//        }

        if (mainActivity == null) {
            mainActivity = MainActivity.getInstance();
        }
        settingsViewModel = mainActivity.getSettingsViewModel();

        binding = FragmentCameraDiscoveryBinding.inflate(inflater, container, false);

        mainActivity.getSupportActionBar().setTitle(R.string.title_camera_discovery);
        cameraArray = new ArrayList<>();

        mCameraAdapter = new CameraDiscoveryAdapter(getContext(), cameraArray);

        //get the settings model
        settings = mainActivity.getSettings();
        binding.setSettings(settings);

        binding.btnCancelSave.btnSave.setOnClickListener(this);
        binding.btnCancelSave.btnCancel.setOnClickListener(this);
        binding.btnScan.setOnClickListener(this);
        binding.lvCameraDiscovery.setOnItemClickListener(this);


        binding.lvCameraDiscovery.setAdapter(mCameraAdapter);

        cameraArray = new ArrayList<Pair<String, String>>();
        resolveSemaphore = new Semaphore(1);

        initializeDiscoveryListener();
        //initializeResolveListener();
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        return binding.getRoot();
    }

    private void initializeDiscoveryListener() {

        // Instantiate a new DiscoveryListener
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            //  Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Timber.d("\\\\NSD\\\\ Discovery Started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found!  Do something with it.
                String name = service.getServiceName();
                String type = service.getServiceType();
                Timber.d("\\\\NSD\\\\ Service Name=" + name);
                Timber.d("\\\\NSD\\\\ Service Type=" + type);
                if (type.equals(Constants.SERVICE_TYPE)) {
                    Timber.d("\\\\NSD\\\\ Service Found @ '" + name + "'");
                    if (!cameraArray.contains(name)) {
                        mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    resolveSemaphore.acquire();
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }

                                mNsdManager.resolveService(service, new NsdManager.ResolveListener() {
                                    @Override
                                    public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {
                                        // Called when the resolve fails.  Use the error code to debug.
                                        Timber.e("\\\\NSD\\\\ Resolve failed error = " + errorCode);
                                        resolveSemaphore.release();
                                    }

                                    @Override
                                    public void onServiceResolved(NsdServiceInfo nsdServiceInfo) {
                                        mServiceInfo = nsdServiceInfo;

                                        // Port is being returned as 9. Not needed.
                                        //int port = mServiceInfo.getPort();

                                        InetAddress host = mServiceInfo.getHost();
                                        mCameraAddress = host.getHostAddress();
                                        cameraArray.add(new Pair<>(name, mCameraAddress));
                                        mainActivity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                mCameraAdapter.updateItems(new Pair<>(name, mCameraAddress));
                                            }
                                        });
                                        Timber.d("\\\\NSD\\\\ Resolved address = " + mCameraAddress);
                                        resolveSemaphore.release();
                                    }
                                });
                            }
                        });

                    }
                }
            }
            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Timber.d("\\\\NDS\\\\ Service Lost");
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Timber.d("\\\\NDS\\\\ Service Stopped");
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                mNsdManager.stopServiceDiscovery(this);
                Timber.d("\\\\NDS\\\\ Start Service Discovery Failed, error = %d", errorCode);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                mNsdManager.stopServiceDiscovery(this);
                Timber.d("\\\\NDS\\\\ Stop Service Discovery Failed, error = %d", errorCode);
            }
        };
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.btnSave) {
            if(selectedIPAddess == null || selectedIPAddess.isEmpty()) {
                Toast.makeText(getContext(), "You must select a camera first", Toast.LENGTH_LONG).show();
                return;
            }
            settings.setCameraAddress(selectedIPAddess);
            mainActivity.getNavController().popBackStack();
        } else if(id == R.id.btnCancel) {
            mainActivity.getNavController().popBackStack();
        } else if(id == R.id.btnScan) {
            if(mNsdManager == null) {
                selectedIPAddess = null;
                mNsdManager = (NsdManager) (mainActivity.getSystemService(Context.NSD_SERVICE));
                initializeDiscoveryListener();
                mNsdManager.discoverServices(Constants.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
            }
            return;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Pair<String, String> pair = mCameraAdapter.getItem(position);
        selectedIPAddess = pair.second;
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mNsdManager != null) {
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        }
    }

    /**
     * Called by the {@link MenuHost} right before the {@link Menu} is shown.
     * This should be called when the menu has been dynamically updated.
     *
     * @param menu the menu that is to be prepared
     * @see #onCreateMenu(Menu, MenuInflater)
     */
    @Override
    public void onPrepareMenu(@NonNull Menu menu) {
        MenuProvider.super.onPrepareMenu(menu);
    }

    /**
     * Called by the {@link MenuHost} to allow the {@link MenuProvider}
     * to inflate {@link MenuItem}s into the menu.
     *
     * @param menu         the menu to inflate the new menu items into
     * @param menuInflater the inflater to be used to inflate the updated menu
     */
    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {

    }

    /**
     * Called by the {@link MenuHost} when a {@link MenuItem} is selected from the menu.
     *
     * @param menuItem the menu item that was selected
     * @return {@code true} if the given menu item is handled by this menu provider,
     * {@code false} otherwise
     */
    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();
        if(id == android.R.id.home) {
            mainActivity.getNavController().popBackStack();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Called by the {@link MenuHost} when the {@link Menu} is closed.
     *
     * @param menu the menu that was closed
     */
    @Override
    public void onMenuClosed(@NonNull Menu menu) {
        MenuProvider.super.onMenuClosed(menu);
    }
}
