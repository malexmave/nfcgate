package tud.seemuh.nfcgate.nfc;

import java.util.List;

import tud.seemuh.nfcgate.db.NfcCommEntry;
import tud.seemuh.nfcgate.util.NfcComm;

public class NfcLogReplayer {
    private boolean mReader;
    private List<NfcCommEntry> mReplayLog;
    private int mReplayIndex = 0;

    public NfcLogReplayer(boolean reader, List<NfcCommEntry> replayLog) {
        mReader = reader;
        mReplayLog = replayLog;
    }

    private NfcComm getNext() {
        // next log entry does not exist -> do nothing
        if (mReplayIndex >= mReplayLog.size())
            return null;

        // get next entry
        return mReplayLog.get(mReplayIndex).getNfcComm();
    }

    public NfcComm getResponse(NfcComm request) {
        // get next entry
        NfcComm next = getNext();

        if (request != null && next != null && next.isCard() == request.isCard()) {
            // request matches the log entry we were expecting
            mReplayIndex++;
            return getResponse(null);
        }
        else if (request == null && next != null && next.isCard() != mReader) {
            // next entry matches our type
            mReplayIndex++;
            return next;
        }

        // either wrong request or next log entry does not match our type: wait
        return null;
    }

    public boolean shouldWait() {
        // get next entry
        NfcComm next = getNext();

        // wait if no next entry exists or next entry is not our type
        return next == null || next.isCard() == mReader;
    }
}
