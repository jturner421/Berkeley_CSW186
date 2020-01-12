package edu.berkeley.cs186.database.recovery;

import edu.berkeley.cs186.database.common.Buffer;
import edu.berkeley.cs186.database.common.ByteBuffer;
import edu.berkeley.cs186.database.common.Pair;
import edu.berkeley.cs186.database.concurrency.DummyLockContext;
import edu.berkeley.cs186.database.io.DiskSpaceManager;
import edu.berkeley.cs186.database.memory.BufferManager;
import edu.berkeley.cs186.database.memory.Page;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

class FreePageLogRecord extends LogRecord {
    private long transNum;
    private long pageNum;
    private long prevLSN;

    FreePageLogRecord(long transNum, long pageNum, long prevLSN) {
        super(LogType.FREE_PAGE);
        this.transNum = transNum;
        this.pageNum = pageNum;
        this.prevLSN = prevLSN;
    }

    @Override
    public Optional<Long> getTransNum() {
        return Optional.of(transNum);
    }

    @Override
    public Optional<Long> getPrevLSN() {
        return Optional.of(prevLSN);
    }

    @Override
    public Optional<Long> getPageNum() {
        return Optional.of(pageNum);
    }

    @Override
    public boolean isUndoable() {
        return true;
    }

    @Override
    public boolean isRedoable() {
        return true;
    }

    @Override
    public Pair<LogRecord, Boolean> undo(long lastLSN) {
        return new Pair<>(new UndoFreePageLogRecord(transNum, pageNum, lastLSN, prevLSN), true);
    }

    @Override
    public void redo(DiskSpaceManager dsm, BufferManager bm) {
        super.redo(dsm, bm);

        try {
            Page p = bm.fetchPage(new DummyLockContext(), pageNum, false);
            bm.freePage(p);
            p.unpin();
        } catch (NoSuchElementException e) {
            /* do nothing - page already freed */
        }
    }

    @Override
    public byte[] toBytes() {
        byte[] b = new byte[1 + Long.BYTES + Long.BYTES + Long.BYTES];
        ByteBuffer.wrap(b)
        .put((byte) getType().getValue())
        .putLong(transNum)
        .putLong(pageNum)
        .putLong(prevLSN);
        return b;
    }

    public static Optional<LogRecord> fromBytes(Buffer buf) {
        long transNum = buf.getLong();
        long pageNum = buf.getLong();
        long prevLSN = buf.getLong();
        return Optional.of(new FreePageLogRecord(transNum, pageNum, prevLSN));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        if (!super.equals(o)) { return false; }
        FreePageLogRecord that = (FreePageLogRecord) o;
        return transNum == that.transNum &&
               pageNum == that.pageNum &&
               prevLSN == that.prevLSN;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), transNum, pageNum, prevLSN);
    }

    @Override
    public String toString() {
        return "FreePageLogRecord{" +
               "transNum=" + transNum +
               ", pageNum=" + pageNum +
               ", prevLSN=" + prevLSN +
               ", LSN=" + LSN +
               '}';
    }
}
