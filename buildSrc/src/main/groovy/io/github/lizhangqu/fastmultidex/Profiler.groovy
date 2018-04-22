package io.github.lizhangqu.fastmultidex

import java.text.MessageFormat

/**
 * from atlas
 */
final class Profiler {

    private static final ThreadLocal entryStack = new ThreadLocal()

    Profiler() {

    }

    static void start() {
        start((String) null);
    }

    static void start(String message) {
        entryStack.set(new Entry(message, (Entry) null, (Entry) null))
    }

    static void start(Message message) {
        entryStack.set(new Entry(message, (Entry) null, (Entry) null))
    }

    static void reset() {
        entryStack.set((Object) null)
    }

    static void enter(String message) {
        Entry currentEntry = getCurrentEntry()
        if (currentEntry != null) {
            currentEntry.enterSubEntry(message)
        }
    }

    static void enter(Message message) {
        Entry currentEntry = getCurrentEntry()
        if (currentEntry != null) {
            currentEntry.enterSubEntry(message)
        }
    }

    static void release() {
        Entry currentEntry = getCurrentEntry()
        if (currentEntry != null) {
            currentEntry.release()
        }
    }

    static long getDuration() {
        Entry entry = (Entry) entryStack.get()
        return entry != null ? entry.getDuration() : -1L
    }

    static String dump() {
        return dump("", "")
    }

    static String dump(String prefix) {
        return dump(prefix, prefix)
    }

    static String dump(String prefix1, String prefix2) {
        Entry entry = (Entry) entryStack.get()
        return entry != null ? entry.toString(prefix1, prefix2) : ""
    }

    static Entry getEntry() {
        return (Entry) entryStack.get()
    }

    private static Entry getCurrentEntry() {
        Entry subEntry = (Entry) entryStack.get()
        Entry entry = null
        if (subEntry != null) {
            while (subEntry != null) {
                entry = subEntry
                subEntry = subEntry.getUnreleasedEntry()
            }
        }
        return entry
    }

    interface Message {

        String getBriefMessage()

        String getDetailedMessage()
    }

    static final class Entry {
        private final List subEntries
        private final Object message
        private final Entry parentEntry
        private final Entry firstEntry
        private final long baseTime
        private final long startTime
        private long endTime

        Entry(Object message, Entry parentEntry, Entry firstEntry) {
            this.subEntries = new ArrayList(4)
            this.message = message
            this.startTime = System.currentTimeMillis()
            this.parentEntry = parentEntry
            this.firstEntry = (Entry) (firstEntry != null ? firstEntry : this)
            this.baseTime = firstEntry == null ? 0L : firstEntry.startTime
        }

        String getMessage() {
            String messageString = null
            if (this.message instanceof String) {
                messageString = (String) this.message
            } else if (this.message instanceof Message) {
                Message messageObject = (Message) this.message
                messageString = messageObject.getDetailedMessage()
            }
            return messageString != null && messageString.length() > 0 ? messageString : (String) null
        }

        long getStartTime() {
            return this.baseTime > 0L ? this.startTime - this.baseTime : 0L
        }

        long getEndTime() {
            return this.endTime < this.baseTime ? -1L : this.endTime - this.baseTime
        }

        long getDuration() {
            return this.endTime < this.startTime ? -1L : this.endTime - this.startTime
        }

        long getDurationOfSelf() {
            long duration = this.getDuration()
            if (duration < 0L) {
                return -1L
            } else if (this.subEntries.isEmpty()) {
                return duration
            } else {
                for (int i = 0; i < this.subEntries.size(); ++i) {
                    Entry subEntry = (Entry) this.subEntries.get(i)
                    duration -= subEntry.getDuration()
                }
                return duration < 0L ? -1L : duration
            }
        }

        double getPecentage() {
            double parentDuration = 0.0D
            double duration = (double) this.getDuration()
            if (this.parentEntry != null && this.parentEntry.isReleased()) {
                parentDuration = (double) this.parentEntry.getDuration()
            }
            return duration > 0.0D && parentDuration > 0.0D ? duration / parentDuration : 0.0D
        }

        double getPecentageOfAll() {
            double firstDuration = 0.0D
            double duration = (double) this.getDuration()
            if (this.firstEntry != null && this.firstEntry.isReleased()) {
                firstDuration = (double) this.firstEntry.getDuration()
            }
            return duration > 0.0D && firstDuration > 0.0D ? duration / firstDuration : 0.0D
        }

        List getSubEntries() {
            return Collections.unmodifiableList(this.subEntries)
        }

        void release() {
            this.endTime = System.currentTimeMillis()
        }

        boolean isReleased() {
            return this.endTime > 0L
        }

        void enterSubEntry(Object message) {
            Entry subEntry = new Entry(message, this, this.firstEntry)
            this.subEntries.add(subEntry)
        }

        Entry getUnreleasedEntry() {
            Entry subEntry = null
            if (!this.subEntries.isEmpty()) {
                subEntry = (Entry) this.subEntries.get(this.subEntries.size() - 1)
                if (subEntry.isReleased()) {
                    subEntry = null
                }
            }
            return subEntry
        }

        @Override
        String toString() {
            return this.toString("", "")
        }

        String toString(String prefix1, String prefix2) {
            StringBuffer buffer = new StringBuffer()
            this.toString(buffer, prefix1, prefix2)
            return buffer.toString()
        }

        void toString(StringBuffer buffer, String prefix1, String prefix2) {
            buffer.append(prefix1)
            String message = this.getMessage()
            long startTime = this.getStartTime()
            long duration = this.getDuration()
            long durationOfSelf = this.getDurationOfSelf()
            double percent = this.getPecentage()
            double percentOfAll = this.getPecentageOfAll()
            Object[] params = [
                    message, new Long(startTime), new Long(duration), new Long(durationOfSelf),
                    new Double(percent), new Double(percentOfAll)
            ]
            StringBuffer pattern = new StringBuffer("{1,number} ")
            if (this.isReleased()) {
                pattern.append("[{2,number}ms")
                if (durationOfSelf > 0L && durationOfSelf != duration) {
                    pattern.append(" ({3,number}ms)")
                }

                if (percent > 0.0D) {
                    pattern.append(", {4,number,##%}")
                }

                if (percentOfAll > 0.0D) {
                    pattern.append(", {5,number,##%}")
                }

                pattern.append("]")
            } else {
                pattern.append("[UNRELEASED]")
            }

            if (message != null) {
                pattern.append(" - {0}")
            }

            buffer.append(MessageFormat.format(pattern.toString(), params))

            for (int i = 0; i < this.subEntries.size(); ++i) {
                Entry subEntry = (Entry) this.subEntries.get(i)
                buffer.append('\n')
                if (i == this.subEntries.size() - 1) {
                    subEntry.toString(buffer, prefix2 + "`---", prefix2 + "    ")
                } else if (i == 0) {
                    subEntry.toString(buffer, prefix2 + "+---", prefix2 + "|   ")
                } else {
                    subEntry.toString(buffer, prefix2 + "+---", prefix2 + "|   ")
                }
            }
        }
    }
}
