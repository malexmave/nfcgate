package de.tu_darmstadt.seemoo.nfcgate.gui.log;

import androidx.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.tu_darmstadt.seemoo.nfcgate.R;
import de.tu_darmstadt.seemoo.nfcgate.db.NfcCommEntry;
import de.tu_darmstadt.seemoo.nfcgate.db.SessionLog;
import de.tu_darmstadt.seemoo.nfcgate.db.model.SessionLogEntryViewModel;
import de.tu_darmstadt.seemoo.nfcgate.db.model.SessionLogEntryViewModelFactory;
import de.tu_darmstadt.seemoo.nfcgate.gui.component.CustomArrayAdapter;
import de.tu_darmstadt.seemoo.nfcgate.nfc.config.ConfigBuilder;
import de.tu_darmstadt.seemoo.nfcgate.util.NfcComm;

import static de.tu_darmstadt.seemoo.nfcgate.util.Utils.bytesToHexDump;

public class SessionLogEntryFragment extends Fragment {
    public enum Type {
        // look at a prerecorded log
        VIEW,
        // like VIEW with the ability to confirm or reject this log
        SELECT,
        // like view but with autoscroll and updates
        LIVE
    }

    // UI references
    ListView mLogEntries;

    private SessionLogEntryListAdapter mLogEntriesAdapter;
    private long mSessionId;
    private Type mType;

    // current data
    private LogAction mLogAction;
    private final List<NfcComm> mLogData = new ArrayList<>();
    private SessionLog mSessionLog;

    // callback
    public interface LogSelectedCallback {
        void onLogSelected(long sessionId);
    }
    LogSelectedCallback mCallback;

    public static SessionLogEntryFragment newInstance(long sessionId, Type type, LogSelectedCallback cb) {
        return new SessionLogEntryFragment()
                .setup(sessionId, type, cb);
    }

    SessionLogEntryFragment setup(long sessionId, Type type, LogSelectedCallback cb) {
        mSessionId = sessionId;
        mType = type;
        mCallback = cb;
        return this;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_log_entry, container, false);

        // setup
        mLogEntries = v.findViewById(R.id.log_entries);
        mLogAction = new LogAction(this);

        // enable custom toolbar actions
        setHasOptionsMenu(true);

        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        // view requires a back button
        if (mType == Type.VIEW) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        // setup db model
        // db data
        final SessionLogEntryViewModel mLogEntryModel = ViewModelProviders.of(this, new SessionLogEntryViewModelFactory(getActivity().getApplication(), mSessionId))
                .get(SessionLogEntryViewModel.class);

        mLogEntryModel.getSession().observe(getViewLifecycleOwner(), sessionLogJoin -> {
            mLogEntriesAdapter.clear();
            mLogData.clear();

            if (sessionLogJoin != null) {
                // save current log data
                mSessionLog = sessionLogJoin.getSessionLog();
                for (NfcCommEntry nfcCommEntry : sessionLogJoin.getNfcCommEntries())
                    mLogData.add(nfcCommEntry.getNfcComm());

                // add log data to list adapter
                mLogEntriesAdapter.addAll(mLogData);
                mLogEntriesAdapter.notifyDataSetChanged();

                // live requires autoscroll, view and select require subtitle
                if (mType == Type.LIVE)
                    mLogEntries.setSelection(mLogEntriesAdapter.getCount() - 1);
                else
                    actionBar.setSubtitle(mSessionLog.toString());
            }
        });

        // setup db data and view adapter
        mLogEntriesAdapter = new SessionLogEntryListAdapter(getActivity(), R.layout.list_log_entry);
        mLogEntries.setAdapter(mLogEntriesAdapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // select mode requires an accept action, view mode a share action
        if (mType == Type.SELECT)
            inflater.inflate(R.menu.toolbar_log_choose, menu);
        else if (mType == Type.VIEW)
            inflater.inflate(R.menu.toolbar_log_view, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (android.R.id.home == item.getItemId()) {
            getActivity().onBackPressed();
            return true;
        } else if (R.id.action_yes == item.getItemId()) {
            mCallback.onLogSelected(mSessionId);
            return true;
        } else if (R.id.action_share == item.getItemId()) {
            mLogAction.share(mSessionLog, mLogData);
            return true;
        } else if (R.id.action_delete == item.getItemId()) {
            mLogAction.delete(mSessionLog);
            getActivity().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class SessionLogEntryListAdapter extends CustomArrayAdapter<NfcComm> {
        SessionLogEntryListAdapter(@NonNull Context context, int resource) {
            super(context, resource);
        }

        @DrawableRes
        private int byCard(boolean card) {
            return card ? R.drawable.ic_tag_grey_60dp : R.drawable.ic_reader_grey_60dp;
        }

        private String byInitial(boolean initial, byte[] data) {
            return initial ? new ConfigBuilder(data).toString() : bytesToHexDump(data);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            final NfcComm comm = getItem(position);

            // set image indicating card or reader
            v.<ImageView>findViewById(R.id.type).setImageResource(byCard(comm.isCard()));
            // set content to either config stream or binary content
            v.<TextView>findViewById(R.id.data).setText(byInitial(comm.isInitial(), comm.getData()));
            // set timestamp
            v.<TextView>findViewById(R.id.timestamp).setText(SessionLog.isoDateFormatter().format(new Date(comm.getTimestamp())));

            return v;
        }
    }
}
