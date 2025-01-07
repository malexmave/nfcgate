package de.tu_darmstadt.seemoo.nfcgate.gui.fragment;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import de.tu_darmstadt.seemoo.nfcgate.R;
import de.tu_darmstadt.seemoo.nfcgate.gui.component.CustomArrayAdapter;
import de.tu_darmstadt.seemoo.nfcgate.gui.component.ContentShare;
import de.tu_darmstadt.seemoo.nfcgate.gui.component.StatusItem;
import de.tu_darmstadt.seemoo.nfcgate.nfc.NfcManager;
import de.tu_darmstadt.seemoo.nfcgate.nfc.chip.NfcChip;
import de.tu_darmstadt.seemoo.nfcgate.util.DeviceNames;

public class StatusFragment extends BaseFragment {
    // ui references
    private ListView mStatus;
    private StatusListAdapter mStatusAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_status, container, false);

        // setup listview
        mStatus = v.findViewById(R.id.status_list);

        // custom toolbar actions
        setHasOptionsMenu(true);
        // set version as subtitle
        getMainActivity().getSupportActionBar().setSubtitle(getString(R.string.about_version, AboutFragment.getVersionNameGit()));

        // handlers
        mStatus.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @DrawableRes
            private int byState(StatusItem.State state) {
                switch (state) {
                    default:
                    case WARN:
                        return R.drawable.ic_warning_grey_24dp;
                    case ERROR:
                        return R.drawable.ic_error_grey_24dp;
                }
            }

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (position >= 0) {
                    final StatusItem item = mStatusAdapter.getItem(position);

                    if (item.getState() != StatusItem.State.OK) {
                        new AlertDialog.Builder(getActivity())
                                .setTitle(getString(item.getState() == StatusItem.State.WARN ?
                                        R.string.status_warning : R.string.status_error))
                                .setPositiveButton(getString(R.string.button_ok), null)
                                .setIcon(byState(item.getState()))
                                .setMessage(item.getMessage())
                                .show();
                    }
                }
            }
        });

        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mStatusAdapter = new StatusListAdapter(getActivity(), R.layout.list_status);
        mStatus.setAdapter(mStatusAdapter);

        detect();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar_status, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_copy) {
            shareConfigAsText();
            return true;
        }
        else if (item.getItemId() == R.id.action_export) {
            shareConfigAsFile();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    String buildConfigContent() {
        // add app version
        final StringBuilder str = new StringBuilder("Version: ")
                .append(AboutFragment.getVersionNameGit());

        // add full device info
        for (int i = 0; i < mStatusAdapter.getCount();  i++)
            str.append("\n").append(mStatusAdapter.getItem(i));

        return str.toString();
    }

    void shareConfigAsText() {
        new ContentShare(getActivity())
                .setMimeType("text/plain")
                .setText(buildConfigContent())
                .share();
    }

    void shareConfigAsFile() {
        new ContentShare(getActivity())
                .setPrefix("config")
                .setExtension(".txt")
                .setMimeType("text/plain")
                .setFile(stream -> stream.write(buildConfigContent().getBytes()))
                .share();
    }

    void detect() {
        mStatusAdapter.add(detectDeviceName());
        mStatusAdapter.add(detectAndroidVersion());
        mStatusAdapter.add(detectBuildNumber());
        mStatusAdapter.add(detectNfcEnabled());
        mStatusAdapter.add(detectHceCapability());
        mStatusAdapter.add(detectModuleEnabled());
        mStatusAdapter.add(detectNativeHookEnabled());
        mStatusAdapter.add(detectNfcModel());

        mStatusAdapter.notifyDataSetChanged();
    }

    StatusItem detectDeviceName() {
        // transform code name into market name
        String deviceString = new DeviceNames(getContext()).formatCurrentDeviceName();

        // device name should be OK for all supported devices
        StatusItem result = new StatusItem(getContext(), getString(R.string.status_devname)).setValue(deviceString);

        // No hist byte on this specific combination
        if ("Nexus 5X".equals(Build.MODEL) && Build.VERSION.RELEASE.equals("6.0.1"))
            result.setWarn(getString(R.string.warn_5X601));

        return result;
    }

    StatusItem detectAndroidVersion() {
        // android version should be OK for all supported versions
        StatusItem result = new StatusItem(getContext(), getString(R.string.status_version)).setValue(Build.VERSION.RELEASE);

        // Android 16 and above is untested
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.VANILLA_ICE_CREAM)
            result.setWarn(getString(R.string.warn_AV));

        return result;
    }

    StatusItem detectBuildNumber() {
        // build number
        StatusItem result = new StatusItem(getContext(), getString(R.string.status_build)).setValue(Build.DISPLAY);

        return result;
    }

    StatusItem detectNfcEnabled() {
        // NFC capability and enabled
        boolean hasNfc = getNfc().isEnabled();
        // NFC Capability should be OK if it is enabled
        StatusItem result = new StatusItem(getContext(), getString(R.string.status_nfc)).setValue(hasNfc);

        if (!hasNfc)
            result.setError(getString(R.string.error_NFCCAP));

        return result;
    }

    StatusItem detectHceCapability() {
        // HCE capability
        boolean hasHCE = getNfc().hasHce();
        // HCE Capability
        StatusItem result = new StatusItem(getContext(), getString(R.string.status_hce)).setValue(hasHCE);

        if (!hasHCE)
            result.setWarn(getString(R.string.warn_HCE));

        return result;
    }

    StatusItem detectModuleEnabled() {
        // xposed module enabled
        boolean hasModule = NfcManager.isModuleLoaded();
        // Xposed module should be OK if it is enabled
        StatusItem result = new StatusItem(getContext(), getString(R.string.status_xposed)).setValue(hasModule);

        if (!hasModule)
            result.setWarn(getString(R.string.warn_XPOMOD));

        return result;
    }

    StatusItem detectNativeHookEnabled() {
        // native hook enabled
        boolean hasNativeHook = getNfc().isHookEnabled();
        // native hook is OK if enabled
        StatusItem result = new StatusItem(getContext(), getString(R.string.status_hook)).setValue(hasNativeHook);

        if (!hasNativeHook)
            result.setWarn(getString(R.string.warn_NATMOD));

        return result;
    }

    StatusItem detectNfcModel() {
        // null or chip model name
        String chipName = NfcChip.detect();
        // Chip model should be OK if it can be detected
        StatusItem result = new StatusItem(getContext(), getString(R.string.status_chip))
                .setValue(chipName != null ? chipName : getString(R.string.status_unknown));

        if (chipName == null)
            result.setWarn(getString(R.string.warn_NFCMOD));

        return result;
    }

    private static class StatusListAdapter extends CustomArrayAdapter<StatusItem> {
        StatusListAdapter(@NonNull Context context, int resource) {
            super(context, resource);
        }

        @DrawableRes
        private int byState(StatusItem.State state) {
            switch (state) {
                default:
                case OK:
                    return R.drawable.ic_check_circle_green_24dp;
                case WARN:
                    return R.drawable.ic_help_orange_24dp;
                case ERROR:
                    return R.drawable.ic_error_red_24dp;
            }
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            final StatusItem item = getItem(position);

            v.<TextView>findViewById(R.id.status_name).setText(item.getName());
            v.<TextView>findViewById(R.id.status_value).setText(item.getValue());
            v.<ImageView>findViewById(R.id.status_icon).setImageResource(byState(item.getState()));

            return v;
        }
    }
}
